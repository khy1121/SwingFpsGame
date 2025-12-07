import React, { useState } from 'react';

const ProjectReportSection = () => {
  const [expandedWeek, setExpandedWeek] = useState(null);

  const projectStats = [
    { label: '개발 기간', value: '2개월', desc: '2025.10 ~ 12' },
    { label: '코드 라인', value: '15,000+', desc: '51개 Java 파일' },
    { label: '캐릭터', value: '10개', desc: '4개 완성, 6개 구현' },
    { label: '맵', value: '4종', desc: 'JSON 기반 편집 가능' }
  ];

  const weeklyProgress = [
    {
      week: '11월 1주차',
      date: '2025-11-07',
      phase: '로비 시스템',
      color: 'blue',
      achievements: [
        '팀 선택 UI 개발 (RED/BLUE)',
        'Ready 시스템 구현',
        '게임 시작 검증 로직',
        'FilledButton 커스텀 컴포넌트 개발'
      ],
      protocols: ['TEAM', 'READY/UNREADY', 'TEAM_ROSTER', 'START', 'GAME_START'],
      challenges: [
        'BorderLayout에서 컴포넌트 중첩 문제 → midPanel 래퍼로 해결',
        'JButton 흰색 배경 오버레이 → FilledButton 커스텀 클래스 개발'
      ]
    },
    {
      week: '11월 2-3주차',
      date: '2025-11-17',
      phase: '고급 스킬 시스템',
      color: 'purple',
      achievements: [
        'Technician 지뢰/터렛 설치 시스템',
        'General 오라 버프 기능',
        'General 에어스트라이크 구현',
        '서버 권위 기반 오브젝트 판정'
      ],
      protocols: ['PLACE', 'OBJ_UPDATE', 'OBJ_DESTROY', 'TURRET_SHOOT', 'BUFF', 'UNBUFF', 'STRIKE_MARK', 'STRIKE_IMPACT'],
      challenges: [
        '설치형 오브젝트 실시간 동기화',
        '복합 이벤트 프로토콜 설계',
        '클라이언트-서버 일관성 유지'
      ]
    },
    {
      week: '11월 4주차',
      date: '2025-11-20',
      phase: 'UI/UX 개선',
      color: 'green',
      achievements: [
        'BulldogBarrageEffect 시각 효과',
        '라운드 정보 표시 강화',
        '설치형 오브젝트 시각화 개선',
        '로비 시스템 UI 개선'
      ],
      protocols: [],
      challenges: [
        '시각 효과의 게임 몰입도 영향 발견',
        '페이드인/아웃 애니메이션 최적화',
        '화면 최적화 isOnScreen() 메서드 활용'
      ]
    },
    {
      week: '12월 1주차',
      date: '2025-12-02',
      phase: '코드 정리 & 최적화',
      color: 'orange',
      achievements: [
        'final 키워드 추가 (8개 필드)',
        '에러 로깅 개선 (stderr 출력)',
        '미사용 상수/변수 제거',
        '코드 안정성 향상'
      ],
      protocols: [],
      challenges: [
        'LOW 우선순위 작업 체계적 정리',
        '불변 필드 식별 및 final 적용',
        '로그 시스템 표준화'
      ]
    },
    {
      week: '12월 2주차',
      date: '2025-12-03',
      phase: '아키텍처 리팩토링',
      color: 'red',
      achievements: [
        'GameRenderer 클래스 추출 (~764줄)',
        'RenderContext 패턴 도입',
        'GamePanel 800줄 감소',
        '중복 필드 6개 제거',
        'Single Source of Truth 확립'
      ],
      protocols: [],
      challenges: [
        'SOLID 원칙 준수하는 MVC 패턴 적용',
        'GameState 중앙 관리 체계 구축',
        '동기화 코드 ~20곳 제거'
      ]
    }
  ];

  const colorMap = {
    blue: { bg: 'bg-blue-50', border: 'border-blue-300', text: 'text-blue-900', badge: 'bg-blue-600' },
    purple: { bg: 'bg-purple-50', border: 'border-purple-300', text: 'text-purple-900', badge: 'bg-purple-600' },
    green: { bg: 'bg-green-50', border: 'border-green-300', text: 'text-green-900', badge: 'bg-green-600' },
    orange: { bg: 'bg-orange-50', border: 'border-orange-300', text: 'text-orange-900', badge: 'bg-orange-600' },
    red: { bg: 'bg-red-50', border: 'border-red-300', text: 'text-red-900', badge: 'bg-red-600' }
  };

  return (
    <div className="space-y-8 animate-fade-in">
      {/* Header */}
      <div className="bg-gradient-to-r from-blue-600 to-purple-600 rounded-xl shadow-lg p-8 text-white">
        <h2 className="text-3xl font-bold mb-3">NetFPS 프로젝트 개발 일지</h2>
        <p className="text-blue-100 text-lg">주차별 작업 내역과 해결 과정</p>
      </div>

      {/* Project Stats */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {projectStats.map((stat, i) => (
          <div key={i} className="bg-white rounded-xl shadow-sm p-6 border border-slate-100 text-center">
            <div className="text-3xl font-bold text-blue-600 mb-2">{stat.value}</div>
            <div className="text-sm font-bold text-slate-800 mb-1">{stat.label}</div>
            <div className="text-xs text-slate-500">{stat.desc}</div>
          </div>
        ))}
      </div>

      {/* Timeline */}
      <div className="bg-white rounded-xl shadow-sm p-6 border border-slate-100">
        <h3 className="text-xl font-bold text-slate-800 mb-6">📅 개발 타임라인</h3>
        
        <div className="relative">
          {/* Timeline Line */}
          <div className="absolute left-8 top-0 bottom-0 w-0.5 bg-slate-200"></div>
          
          <div className="space-y-8">
            {weeklyProgress.map((week, index) => {
              const colors = colorMap[week.color];
              const isExpanded = expandedWeek === index;
              
              return (
                <div key={index} className="relative pl-20">
                  {/* Timeline Dot */}
                  <div className={`absolute left-6 top-2 w-5 h-5 ${colors.badge} rounded-full border-4 border-white shadow-md`}></div>
                  
                  {/* Content Card */}
                  <div className={`${colors.bg} border-2 ${colors.border} rounded-lg overflow-hidden`}>
                    <button
                      onClick={() => setExpandedWeek(isExpanded ? null : index)}
                      className="w-full p-5 text-left hover:opacity-80 transition-opacity"
                    >
                      <div className="flex items-center justify-between mb-2">
                        <div className="flex items-center gap-3">
                          <span className={`px-3 py-1 ${colors.badge} text-white text-xs font-bold rounded-full`}>
                            {week.week}
                          </span>
                          <span className="text-sm text-slate-500">{week.date}</span>
                        </div>
                        <span className={`text-2xl transform transition-transform ${isExpanded ? 'rotate-180' : ''}`}>
                          ▼
                        </span>
                      </div>
                      <h4 className={`text-xl font-bold ${colors.text}`}>{week.phase}</h4>
                    </button>
                    
                    {isExpanded && (
                      <div className="px-5 pb-5 space-y-4 animate-fade-in">
                        {/* Achievements */}
                        <div>
                          <h5 className={`font-bold ${colors.text} mb-2 flex items-center gap-2`}>
                            <span>🎯</span> 주요 성과
                          </h5>
                          <ul className="space-y-1">
                            {week.achievements.map((achievement, i) => (
                              <li key={i} className={`text-sm ${colors.text} flex items-start gap-2`}>
                                <span className="mt-1">•</span>
                                <span>{achievement}</span>
                              </li>
                            ))}
                          </ul>
                        </div>
                        
                        {/* Protocols */}
                        {week.protocols.length > 0 && (
                          <div>
                            <h5 className={`font-bold ${colors.text} mb-2 flex items-center gap-2`}>
                              <span>📡</span> 추가 프로토콜
                            </h5>
                            <div className="flex flex-wrap gap-2">
                              {week.protocols.map((protocol, i) => (
                                <span key={i} className="px-2 py-1 bg-white text-slate-700 text-xs font-mono rounded border border-slate-300">
                                  {protocol}
                                </span>
                              ))}
                            </div>
                          </div>
                        )}
                        
                        {/* Challenges */}
                        <div>
                          <h5 className={`font-bold ${colors.text} mb-2 flex items-center gap-2`}>
                            <span>🔧</span> 해결 과제
                          </h5>
                          <ul className="space-y-2">
                            {week.challenges.map((challenge, i) => (
                              <li key={i} className={`text-sm ${colors.text} p-2 bg-white rounded border border-slate-200`}>
                                {challenge}
                              </li>
                            ))}
                          </ul>
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>

      {/* Final Achievements */}
      <div className="bg-gradient-to-r from-green-600 to-emerald-600 rounded-xl shadow-lg p-6 text-white">
        <h3 className="text-xl font-bold mb-4">🏆 최종 성과</h3>
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <div className="text-center p-4 bg-white/10 rounded-lg">
            <div className="text-3xl font-bold mb-1">800줄</div>
            <div className="text-sm text-green-100">코드 감소</div>
          </div>
          <div className="text-center p-4 bg-white/10 rounded-lg">
            <div className="text-3xl font-bold mb-1">SOLID</div>
            <div className="text-sm text-green-100">원칙 준수</div>
          </div>
          <div className="text-center p-4 bg-white/10 rounded-lg">
            <div className="text-3xl font-bold mb-1">MVC</div>
            <div className="text-sm text-green-100">패턴 적용</div>
          </div>
          <div className="text-center p-4 bg-white/10 rounded-lg">
            <div className="text-3xl font-bold mb-1">15K+</div>
            <div className="text-sm text-green-100">코드 라인</div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ProjectReportSection;
