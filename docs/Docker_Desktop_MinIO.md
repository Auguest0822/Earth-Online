# Docker Desktop + MinIO 部署问题排查记录

本文档记录了在 Windows 系统上安装 Docker Desktop、配置 WSL 2 后端，并成功运行 MinIO 容器过程中遇到的所有问题及解决方案。

---

## 1. 下载 Docker Desktop 版本选择

**问题**：电脑是 Windows，官网提供多个下载选项，应选择哪一个？

**解决**：选择 **Download for Windows – AMD64**（绝大多数 Intel/AMD 处理器的 Windows 电脑均属此架构）。可通过“此电脑 → 属性 → 系统类型”确认是否为“基于 x64 的处理器”。

---

## 2. 安装 Docker Desktop 时选择安装模式

**问题**：安装过程中出现“Per-user installation”与“All-users installation”选项。

**解决**：选择 **Per-user installation (Recommended)**。该模式默认使用 WSL 2 后端，无需管理员权限，适合绝大多数开发场景。同时勾选“Add shortcut to desktop”方便使用。

---

## 3. Docker Desktop 启动失败，提示 WSL 未安装

**现象**：启动 Docker Desktop 后弹窗“WSL not installed”。

**解决**：
- 以管理员身份打开 PowerShell，执行：
  ```powershell
  wsl --install

## 4. 安装 WSL 时网络超时（错误代码 0x80072f78 或 WININET_E_TIMEOUT）
现象：

text
复制
下载
错误代码: Wsl/InstallDistro/0x80072f78
无法从 'https://raw.githubusercontent.com/...' 提取通讯组列表。操作超时
解决：

确保系统功能已启用（管理员 PowerShell）：

powershell
复制
下载
dism.exe /online /enable-feature /featurename:Microsoft-Windows-Subsystem-Linux /all /norestart
dism.exe /online /enable-feature /featurename:VirtualMachinePlatform /all /norestart
重启电脑。

如仍因网络问题无法安装，改用手动导入法将 Ubuntu 安装到 E 盘（避开 GitHub 域名访问）：

从国内镜像下载 Ubuntu 的 rootfs .tar 文件（如阿里云镜像 ubuntu-22.04-server-cloudimg-amd64-root.tar.xz，解压得到 .tar）

在 E 盘创建目录 E:\WSL\Ubuntu

执行导入：

powershell
复制
下载
wsl --import Ubuntu E:\WSL\Ubuntu E:\下载的ubuntu.tar --version 2
设置默认用户（可选）：wsl -d Ubuntu 进入后手动 useradd 创建用户。

## 5. WSL 中无法使用 docker 命令
现象：在 Ubuntu 终端输入 docker --version 提示命令未找到。

解决：在 Docker Desktop 中启用 WSL 集成：

右键系统托盘 Docker 鲸鱼图标 → Settings → Resources → WSL Integration

开启 “Enable integration with my default WSL distro”

勾选已安装的 Ubuntu 发行版

点击 Apply & Restart

## 6. Docker 命令报错 permission denied while trying to connect to the Docker API
现象：

text
复制
下载
permission denied while trying to connect to the docker API at unix:///var/run/docker.sock
解决：将当前用户加入 docker 组：

bash
复制
下载
sudo usermod -aG docker $USER
生效方法：退出当前 WSL 会话（exit）并重新进入（wsl -d Ubuntu），组权限即可生效。

## 7. MinIO 容器创建后立即退出（docker ps 无容器）
现象：执行 docker run 后返回容器 ID，但 docker ps 看不到运行中的容器。

排查：查看容器日志

bash
复制
下载
docker logs earth-online-minio
日志报错：

text
复制
下载
FATAL Unable to start listening on console port: address ::9001: too many colons in address
原因：--console-address "::9001" 格式不被 MinIO 正确解析。

解决：将地址改为 :9001（表示监听所有接口）：

bash
复制
下载
docker run -d --name earth-online-minio \
  -e "MINIO_ROOT_USER=earth-online-admin" \
  -e "MINIO_ROOT_PASSWORD=12345678" \
  -v earth-online-minio-data:/data \
  quay.io/minio/minio server /data --console-address ":9001"
## 8. 容器运行正常但浏览器无法访问控制台（ERR_CONNECTION_REFUSED）
现象：docker ps 显示容器状态 Up，但 PORTS 列只显示 9000/tcp，没有主机端口映射；浏览器访问 http://localhost:9001 拒绝连接。

原因：未使用 -p 参数将容器端口映射到宿主机。

解决：重新创建容器并添加端口映射（同时映射 API 端口 9000 和控制台端口 9001）：

bash
复制
下载
docker rm -f earth-online-minio
docker run -d \
  --name earth-online-minio \
  -p 9000:9000 \
  -p 9001:9001 \
  -e "MINIO_ROOT_USER=earth-online-admin" \
  -e "MINIO_ROOT_PASSWORD=12345678" \
  -v earth-online-minio-data:/data \
  quay.io/minio/minio server /data --console-address ":9001"
验证：

docker ps 应显示 0.0.0.0:9000-9001->9000-9001/tcp

浏览器访问 http://localhost:9001，使用设置的账号密码登录。

## 9. 最终成功访问 MinIO 控制台
控制台地址：http://localhost:9001

用户名：earth-online-admin

密码：12345678

登录后即可创建 Bucket，开始使用对象存储服务。

附：常用 Docker 命令备忘
命令	说明
docker ps	查看运行中的容器
docker ps -a	查看所有容器（含已停止的）
docker logs <容器名>	查看容器日志
docker stop <容器名>	停止容器
docker start <容器名>	启动已停止的容器
docker rm -f <容器名>	强制删除容器
docker exec -it <容器名> /bin/bash	进入容器内部 Shell
docker system prune -a	清理未使用的镜像、容器、网络等（释放磁盘空间）