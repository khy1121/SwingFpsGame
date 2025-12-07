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

      {/* Architecture Deep Dive */}
      <div className="bg-white rounded-xl shadow-sm p-6 border border-slate-100">
        <h3 className="text-xl font-bold text-slate-800 mb-6">🏗️ 아키텍처 심층 분석</h3>
        
        {/* MVC Pattern */}
        <div className="mb-6">
          <h4 className="font-bold text-slate-700 mb-3 flex items-center gap-2">
            <span className="text-2xl">🎯</span> MVC 패턴 적용
          </h4>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="bg-blue-50 p-4 rounded-lg border border-blue-200">
              <div className="font-bold text-blue-900 mb-2">Model</div>
              <div className="text-sm text-blue-800 space-y-1">
                <div>• GameState (게임 상태)</div>
                <div>• CharacterData (캐릭터 정보)</div>
                <div>• PlayerData (플레이어 데이터)</div>
                <div>• Ability (스킬 모델)</div>
              </div>
            </div>
            <div className="bg-green-50 p-4 rounded-lg border border-green-200">
              <div className="font-bold text-green-900 mb-2">View</div>
              <div className="text-sm text-green-800 space-y-1">
                <div>• GameRenderer (렌더링)</div>
                <div>• UIManager (UI 컴포넌트)</div>
                <div>• RenderContext (렌더 데이터)</div>
                <div>• SkillEffects (시각 효과)</div>
              </div>
            </div>
            <div className="bg-purple-50 p-4 rounded-lg border border-purple-200">
              <div className="font-bold text-purple-900 mb-2">Controller</div>
              <div className="text-sm text-purple-800 space-y-1">
                <div>• GamePanel (게임 루프)</div>
                <div>• InputController (입력)</div>
                <div>• GameMessageHandler (메시지)</div>
                <div>• NetworkClient (네트워크)</div>
              </div>
            </div>
          </div>
        </div>

        {/* Manager Classes */}
        <div className="mb-6">
          <h4 className="font-bold text-slate-700 mb-3 flex items-center gap-2">
            <span className="text-2xl">⚙️</span> 8개 Manager 클래스
          </h4>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {[
              { name: 'CollisionManager', desc: '충돌 감지 및 히트박스 처리', lines: '~200줄' },
              { name: 'PlayerMovementController', desc: '플레이어 이동 및 카메라 제어', lines: '~250줄' },
              { name: 'SpawnManager', desc: '스폰 포인트 관리', lines: '~150줄' },
              { name: 'GameObjectManager', desc: '지뢰/터렛 등 오브젝트 관리', lines: '~300줄' },
              { name: 'MapManager', desc: 'JSON 맵 로딩 및 에디터', lines: '~400줄' },
              { name: 'SkillManager', desc: '스킬 쿨다운 및 실행', lines: '~350줄' },
              { name: 'UIManager', desc: 'HUD, 메뉴, 다이얼로그', lines: '~450줄' },
              { name: 'GameLogicController', desc: '라운드/게임 로직 제어', lines: '~280줄' }
            ].map((manager, i) => (
              <div key={i} className="bg-slate-50 p-3 rounded border border-slate-200">
                <div className="font-mono text-sm font-bold text-slate-800">{manager.name}</div>
                <div className="text-xs text-slate-600 mt-1">{manager.desc}</div>
                <div className="text-xs text-slate-500 mt-1">{manager.lines}</div>
              </div>
            ))}
          </div>
        </div>

        {/* Message Handler System */}
        <div className="mb-6">
          <h4 className="font-bold text-slate-700 mb-3 flex items-center gap-2">
            <span className="text-2xl">📨</span> 메시지 핸들러 시스템
          </h4>
          <div className="bg-indigo-50 p-4 rounded-lg border border-indigo-200">
            <div className="text-sm text-indigo-900 mb-3">
              <strong>GameMessageHandler</strong>가 20+ 프로토콜 메시지를 라우팅하여 게임 상태를 업데이트합니다.
            </div>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-2">
              {[
                'WELCOME', 'TEAM_ROSTER', 'CHAT', 'CHARACTER_SELECT',
                'PLAYER', 'REMOVE', 'KILL', 'STATS',
                'SHOOT', 'SKILL', 'MISSILE', 'HIT',
                'PLACE', 'OBJ_DESTROY', 'OBJ_UPDATE', 'TURRET_SHOOT',
                'BUFF', 'UNBUFF', 'STRIKE_MARK', 'STRIKE_IMPACT',
                'ROUND_WIN', 'ROUND_END', 'MAP_SYNC', 'ROUND_START',
                'GAME_OVER', 'GAME_END', 'MENU_ACTION'
              ].map((protocol, i) => (
                <div key={i} className="bg-white px-2 py-1 rounded text-xs font-mono text-indigo-700 border border-indigo-100">
                  {protocol}
                </div>
              ))}
            </div>
            <div className="mt-3 text-xs text-indigo-700 bg-white p-2 rounded border border-indigo-100 font-mono">
              handleMessage(msg) → switch(command) → handle*() 메서드 호출
            </div>
          </div>
        </div>

        {/* Network Architecture */}
        <div>
          <h4 className="font-bold text-slate-700 mb-3 flex items-center gap-2">
            <span className="text-2xl">🌐</span> 네트워크 아키텍처
          </h4>
          <div className="bg-slate-50 p-4 rounded-lg border border-slate-200">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <div className="font-bold text-slate-800 mb-2">서버 (GameServer)</div>
                <div className="text-sm text-slate-600 space-y-1">
                  <div>• TCP Socket (포트 7777)</div>
                  <div>• 멀티스레드 (각 클라이언트마다 스레드)</div>
                  <div>• ConcurrentHashMap으로 플레이어 관리</div>
                  <div>• 권위 서버 모델 (피해 판정, 스킬 검증)</div>
                  <div>• 브로드캐스트 + 유니캐스트 혼용</div>
                </div>
              </div>
              <div>
                <div className="font-bold text-slate-800 mb-2">클라이언트 (NetworkClient)</div>
                <div className="text-sm text-slate-600 space-y-1">
                  <div>• 논블로킹 입력 스레드</div>
                  <div>• DataInputStream/OutputStream</div>
                  <div>• 60 FPS 위치 동기화</div>
                  <div>• 예측 이동 (클라이언트 사이드)</div>
                  <div>• 200ms 피격 쿨다운 (중복 방지)</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Technical Details */}
      <div className="bg-white rounded-xl shadow-sm p-6 border border-slate-100">
        <h3 className="text-xl font-bold text-slate-800 mb-6">🔧 기술적 세부사항</h3>
        
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Skill System */}
          <div>
            <h4 className="font-bold text-slate-700 mb-3">스킬 시스템</h4>
            <div className="space-y-2 text-sm text-slate-600">
              <div className="bg-purple-50 p-3 rounded border border-purple-200">
                <div className="font-bold text-purple-900">22개 이펙트 클래스</div>
                <div className="text-xs text-purple-700 mt-1">
                  RavenDashEffect, PiperSonarEffect, BulldogBarrageEffect 등
                </div>
              </div>
              <div className="bg-purple-50 p-3 rounded border border-purple-200">
                <div className="font-bold text-purple-900">쿨다운 시스템</div>
                <div className="text-xs text-purple-700 mt-1">
                  전술 스킬 8-12초, 궁극기 20-30초
                </div>
              </div>
              <div className="bg-purple-50 p-3 rounded border border-purple-200">
                <div className="font-bold text-purple-900">팀 버프 공유</div>
                <div className="text-xs text-purple-700 mt-1">
                  Piper 정찰, General 오라 등
                </div>
              </div>
            </div>
          </div>

          {/* Map System */}
          <div>
            <h4 className="font-bold text-slate-700 mb-3">맵 시스템</h4>
            <div className="space-y-2 text-sm text-slate-600">
              <div className="bg-green-50 p-3 rounded border border-green-200">
                <div className="font-bold text-green-900">JSON 기반 데이터</div>
                <div className="text-xs text-green-700 mt-1">
                  walkable 2D 배열 + obstacles 좌표
                </div>
              </div>
              <div className="bg-green-50 p-3 rounded border border-green-200">
                <div className="font-bold text-green-900">실시간 에디터</div>
                <div className="text-xs text-green-700 mt-1">
                  F4 편집 모드, Ctrl+S 저장, F3-F6 단축키
                </div>
              </div>
              <div className="bg-green-50 p-3 rounded border border-green-200">
                <div className="font-bold text-green-900">4종 맵</div>
                <div className="text-xs text-green-700 mt-1">
                  map, map2, map3, village (라운드마다 랜덤)
                </div>
              </div>
            </div>
          </div>

          {/* Collision System */}
          <div>
            <h4 className="font-bold text-slate-700 mb-3">충돌 감지 시스템</h4>
            <div className="space-y-2 text-sm text-slate-600">
              <div className="bg-orange-50 p-3 rounded border border-orange-200">
                <div className="font-bold text-orange-900">타일 기반 충돌</div>
                <div className="text-xs text-orange-700 mt-1">
                  32x32 픽셀 타일, walkable 배열 참조
                </div>
              </div>
              <div className="bg-orange-50 p-3 rounded border border-orange-200">
                <div className="font-bold text-orange-900">히트박스 판정</div>
                <div className="text-xs text-orange-700 mt-1">
                  원형 히트박스 (반경 20px), 거리 계산
                </div>
              </div>
              <div className="bg-orange-50 p-3 rounded border border-orange-200">
                <div className="font-bold text-orange-900">미사일 충돌</div>
                <div className="text-xs text-orange-700 mt-1">
                  벽 충돌 시 제거, 플레이어 충돌 → HITME
                </div>
              </div>
            </div>
          </div>

          {/* Performance */}
          <div>
            <h4 className="font-bold text-slate-700 mb-3">성능 최적화</h4>
            <div className="space-y-2 text-sm text-slate-600">
              <div className="bg-blue-50 p-3 rounded border border-blue-200">
                <div className="font-bold text-blue-900">60 FPS 게임 루프</div>
                <div className="text-xs text-blue-700 mt-1">
                  16ms 타이머, 일정한 프레임 유지
                </div>
              </div>
              <div className="bg-blue-50 p-3 rounded border border-blue-200">
                <div className="font-bold text-blue-900">화면 최적화</div>
                <div className="text-xs text-blue-700 mt-1">
                  isOnScreen() 메서드로 불필요한 렌더링 스킵
                </div>
              </div>
              <div className="bg-blue-50 p-3 rounded border border-blue-200">
                <div className="font-bold text-blue-900">네트워크 효율</div>
                <div className="text-xs text-blue-700 mt-1">
                  200ms 피격 쿨다운, 중복 패킷 방지
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Final Achievements */}
      <div className="bg-gradient-to-r from-green-600 to-emerald-600 rounded-xl shadow-lg p-6 text-white">
        <h3 className="text-xl font-bold mb-4">🏆 최종 성과</h3>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
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
