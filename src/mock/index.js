// 模拟返回包含所有需要的数据
export const getProfileData = () => {
  return {
    userInfo: {
      avatar: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
      nickname: '地球居民 \uD83C\uDF31',
      slogan: '生活不是为了赶路，而是为了感受路上的风景，风...',
      level: 24,
      experience: 65, // percentage
      birthDate: '2001-05-21',
      survivalDays: 8766,
      survivalDuration: '23年11个月15天',
      nextLevelExp: '还需 11 年 升级 Lv.25'
    },
    timelines: [
      {
        year: 2025,
        status: '未完成',
        desc: '这一年尚未到来，敬请期待更多精彩...',
        locked: true
      },
      {
        year: 2024,
        status: 'Lv.23',
        event: '毕业旅行去了日本，第一次一个人出国 ✈️',
        keywords: ['成长', '独立', '冒险'],
        moods: ['\uD83D\uDE0A', '\uD83D\uDE01', '\uD83E\uDD29'],
        photos: [
          'https://images.unsplash.com/photo-1542051842-83b3cd12bb3d?auto=format&fit=crop&q=80&w=150&h=100',
          'https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?auto=format&fit=crop&q=80&w=150&h=100',
          'https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?auto=format&fit=crop&q=80&w=150&h=100'
        ]
      },
      {
        year: 2023,
        status: 'Lv.22',
        event: '换了新工作，认识了很多有趣的人 \uD83D\uDC65',
        keywords: ['工作', '挑战', '学习'],
        moods: ['\uD83D\uDE0A', '\uD83E\uDD13', '\uD83D\uDE0C'],
        photos: [
          'https://images.unsplash.com/photo-1522071820081-009f0129c71c?auto=format&fit=crop&q=80&w=150&h=100',
          'https://images.unsplash.com/photo-1497215728101-856f4ea42174?auto=format&fit=crop&q=80&w=150&h=100'
        ]
      },
      {
        year: 2022,
        status: 'Lv.21',
        event: '养了一只猫咪，它治愈了我 \uD83D\uDC31',
        keywords: ['治愈', '陪伴', '温暖'],
        moods: ['\uD83D\uDE0D', '\uD83D\uDE0A', '\uD83E\uDD70'],
        photos: [
          'https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?auto=format&fit=crop&q=80&w=150&h=100',
          'https://images.unsplash.com/photo-1573865526739-10659fec78a5?auto=format&fit=crop&q=80&w=150&h=100'
        ]
      }
    ],
    eventsData: [
      { value: 32, name: '学习成长' },
      { value: 24, name: '工作事业' },
      { value: 18, name: '旅行探索' },
      { value: 16, name: '生活日常' },
      { value: 10, name: '情感关系' }
    ],
    moodTrend: [
      { year: '2021', score: 6.5 },
      { year: '2022', score: 7.2 },
      { year: '2023', score: 8.5 },
      { year: '2024', score: 9.0 },
      { year: '2025', score: 8.8 }
    ],
    keywords: [
      { name: '成长', value: 100 },
      { name: '旅行', value: 80 },
      { name: '工作', value: 70 },
      { name: '学习', value: 65 },
      { name: '独立', value: 60 },
      { name: '陪伴', value: 55 },
      { name: '治愈', value: 50 },
      { name: '挑战', value: 45 },
      { name: '温暖', value: 40 },
      { name: '快乐', value: 35 }
    ],
    achievements: [
      { icon: 'Trophy', name: '初来乍到', active: true },
      { icon: 'Medal', name: '年度记录者', active: true },
      { icon: 'Star', name: '时间旅人', active: true },
      { icon: 'Opportunity', name: '生活探索家', active: false },
      { icon: 'Avatar', name: '坚持记录者', active: false }
    ],
    capsules: [
      { title: '给未来的自己', desc: '留下一份礼物' }
    ]
  }
}
