import React from 'react';

const DevToolsSection = () => (
  <div className="space-y-8 animate-fade-in">
    <div className="bg-white rounded-xl shadow-sm p-6 border border-slate-100">
      <h2 className="text-2xl font-bold text-slate-800 mb-2">개발 도구 & 유지보수</h2>
      <p className="text-slate-600">다양한 개발 도구를 활용하여 생산성을 극대화했습니다.</p>
    </div>

    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
      {/* Development Tools */}
      <div className="bg-white p-6 rounded-xl shadow-sm border border-slate-100">
        <h3 className="text-lg font-bold text-slate-800 mb-4">Development Tools</h3>
        <div className="space-y-3">
          {[
            { icon: '🗺️', name: 'Tiled Map Editor', desc: 'JSON 기반 맵 데이터 편집' },
            { icon: '🎨', name: 'Piskel', desc: '48x64px 캐릭터 스프라이트 제작' },
            { icon: '💻', name: 'VS Code', desc: 'React 문서 사이트 개발' },
            { icon: '☕', name: 'Eclipse IDE', desc: 'Java 게임 클라이언트/서버 개발' },
            { icon: '🤖', name: 'Claude Sonnet 4.5', desc: '코드 리뷰 및 리팩토링 참여' },
            { icon: '🔧', name: 'Git/GitHub', desc: '버전 관리 및 협업' }
          ].map((tool, i) => (
            <div key={i} className="flex items-center p-3 bg-slate-50 rounded border border-slate-100">
              <span className="text-2xl mr-3">{tool.icon}</span>
              <div className="flex-1">
                <div className="font-bold text-slate-800 text-sm">{tool.name}</div>
                <div className="text-xs text-slate-500">{tool.desc}</div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* In-game Editor Keys */}
      <div className="bg-white p-6 rounded-xl shadow-sm border border-slate-100">
        <h3 className="text-lg font-bold text-slate-800 mb-4">In-game Editor (Function Keys)</h3>
        <div className="grid grid-cols-2 gap-4 mb-4">
          {[
            { key: 'F3', title: 'Debug Grid', desc: '장애물 타일 시각화' },
            { key: 'F4', title: 'Edit Mode', desc: '맵 편집기 활성화' },
            { key: 'F5', title: 'Save Map', desc: 'JSON 파일로 저장' },
            { key: 'F6', title: 'Load Map', desc: '다른 맵으로 전환' }
          ].map((item, i) => (
            <div key={i} className="flex items-center p-3 bg-slate-50 rounded border border-slate-200">
              <kbd className="px-2 py-1 bg-white border border-slate-300 rounded text-sm font-bold text-slate-700 shadow-sm mr-3">{item.key}</kbd>
              <div>
                <div className="font-bold text-slate-800 text-sm">{item.title}</div>
                <div className="text-xs text-slate-500">{item.desc}</div>
              </div>
            </div>
          ))}
        </div>
        
        {/* Paint Mode Keys */}
        <div className="mt-4 p-3 bg-blue-50 rounded border border-blue-100">
          <div className="text-xs font-bold text-blue-700 mb-2">EDIT MODE PAINT (1-4 Keys)</div>
          <div className="grid grid-cols-2 gap-2 text-xs">
            <div className="flex items-center">
              <kbd className="px-2 py-1 bg-white border border-slate-300 rounded text-xs mr-2">1</kbd>
              <span className="text-slate-600">이동 가능</span>
            </div>
            <div className="flex items-center">
              <kbd className="px-2 py-1 bg-white border border-slate-300 rounded text-xs mr-2">2</kbd>
              <span className="text-slate-600">장애물</span>
            </div>
            <div className="flex items-center">
              <kbd className="px-2 py-1 bg-white border border-slate-300 rounded text-xs mr-2">3</kbd>
              <span className="text-red-600">RED 스폰</span>
            </div>
            <div className="flex items-center">
              <kbd className="px-2 py-1 bg-white border border-slate-300 rounded text-xs mr-2">4</kbd>
              <span className="text-blue-600">BLUE 스폰</span>
            </div>
          </div>
        </div>
      </div>

      {/* Future Roadmap */}
      <div className="bg-white p-6 rounded-xl shadow-sm border border-slate-100 md:col-span-2">
        <h3 className="text-lg font-bold text-slate-800 mb-4">Future Roadmap</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
          {[
            { color: 'bg-blue-500', text: 'UDP 프로토콜 도입 (위치 동기화 최적화)', priority: 'HIGH' },
            { color: 'bg-blue-500', text: 'Dedicated Server (클라우드 배포)', priority: 'HIGH' },
            { color: 'bg-blue-500', text: 'Replay System (경기 녹화 및 재생)', priority: 'MEDIUM' },
            { color: 'bg-blue-500', text: '관전 모드 (Observer)', priority: 'MEDIUM' },
            { color: 'bg-slate-300', text: 'Undo/Redo 기능 (맵 에디터)', priority: 'LOW' },
            { color: 'bg-slate-300', text: '브러시 크기 선택 (1x1, 2x2, 3x3)', priority: 'LOW' }
          ].map((item, i) => (
            <div key={i} className="flex items-center justify-between text-slate-600 text-sm p-2 bg-slate-50 rounded">
              <div className="flex items-center">
                <span className={`w-2 h-2 ${item.color} rounded-full mr-3`}></span>
                {item.text}
              </div>
              <span className={`text-xs px-2 py-1 rounded ${item.priority === 'HIGH' ? 'bg-red-100 text-red-700' : item.priority === 'MEDIUM' ? 'bg-yellow-100 text-yellow-700' : 'bg-slate-100 text-slate-600'}`}>
                {item.priority}
              </span>
            </div>
          ))}
        </div>
      </div>

      {/* Testing & Quality Assurance */}
      <div className="bg-white p-6 rounded-xl shadow-sm border border-slate-100 md:col-span-2">
        <h3 className="text-lg font-bold text-slate-800 mb-4">🧪 테스트 & 품질 보증</h3>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className="bg-emerald-50 p-4 rounded-lg border border-emerald-200">
            <div className="font-bold text-emerald-900 mb-2">수동 테스트</div>
            <div className="text-sm text-emerald-800 space-y-1">
              <div>• 4인 동시 접속 테스트</div>
              <div>• 맵별 밸런스 체크</div>
              <div>• 캐릭터 스킬 검증</div>
              <div>• 네트워크 지연 시뮬레이션</div>
            </div>
          </div>
          <div className="bg-blue-50 p-4 rounded-lg border border-blue-200">
            <div className="font-bold text-blue-900 mb-2">코드 리뷰</div>
            <div className="text-sm text-blue-800 space-y-1">
              <div>• Claude AI와 협업 리뷰</div>
              <div>• SOLID 원칙 검증</div>
              <div>• 코드 중복 제거</div>
              <div>• 성능 최적화 제안</div>
            </div>
          </div>
          <div className="bg-purple-50 p-4 rounded-lg border border-purple-200">
            <div className="font-bold text-purple-900 mb-2">버그 추적</div>
            <div className="text-sm text-purple-800 space-y-1">
              <div>• GitHub Issues 활용</div>
              <div>• 주간 보고서 작성</div>
              <div>• 문제점 문서화</div>
              <div>• 해결 과정 기록</div>
            </div>
          </div>
        </div>
      </div>

      {/* Development Workflow */}
      <div className="bg-gradient-to-r from-cyan-600 to-blue-600 rounded-xl shadow-lg p-6 text-white md:col-span-2">
        <h3 className="text-xl font-bold mb-4">🔄 개발 워크플로우</h3>
        <div className="grid grid-cols-1 md:grid-cols-5 gap-4">
          {[
            { step: '1', title: '기능 설계', desc: '요구사항 분석' },
            { step: '2', title: '구현', desc: '코드 작성' },
            { step: '3', title: '테스트', desc: '동작 검증' },
            { step: '4', title: '리뷰', desc: 'AI 코드 리뷰' },
            { step: '5', title: '배포', desc: 'Git Push' }
          ].map((item, i) => (
            <div key={i} className="bg-white/10 p-4 rounded-lg text-center">
              <div className="text-3xl font-bold mb-2">{item.step}</div>
              <div className="font-bold text-sm mb-1">{item.title}</div>
              <div className="text-xs text-cyan-100">{item.desc}</div>
            </div>
          ))}
        </div>
      </div>
    </div>
  </div>
);

export default DevToolsSection;
