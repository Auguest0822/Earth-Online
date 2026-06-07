import { spawn } from 'node:child_process'

const isWindows = process.platform === 'win32'
const npmCommand = 'npm'

const processes = [
  {
    name: 'api',
    child: spawn(npmCommand, ['run', 'api'], {
      stdio: 'inherit',
      shell: isWindows
    })
  },
  {
    name: 'web',
    child: spawn(npmCommand, ['run', 'dev', '--', '--host', '127.0.0.1', '--port', '5173'], {
      stdio: 'inherit',
      shell: isWindows
    })
  }
]

const stopAll = () => {
  processes.forEach(({ child }) => {
    if (!child.killed) child.kill()
  })
}

processes.forEach(({ name, child }) => {
  child.on('exit', (code) => {
    if (code && code !== 0) {
      console.error(`[dev:${name}] exited with code ${code}`)
      stopAll()
      process.exit(code)
    }
  })
})

process.on('SIGINT', () => {
  stopAll()
  process.exit(0)
})

process.on('SIGTERM', () => {
  stopAll()
  process.exit(0)
})
