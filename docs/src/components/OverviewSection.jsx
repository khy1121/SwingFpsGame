import React from 'react';
import ChartComponent from './ChartComponent';

const OverviewSection = () => {
  const chartData = {
    labels: ['Client (Swing/GamePanel)', 'Server (TCP Socket)', 'Managers (8개)', 'Effects (24개)'],
    datasets: [{
      data: [35, 25, 25, 15],
      backgroundColor: ['#3b82f6', '#8b5cf6', '#10b981', '#f59e0b'],
      borderWidth: 0
    }]
  };

  return (
    <div className="space-y-8 animate-fade-in">
      <div className="bg-white rounded-xl shadow-sm p-6 border border-slate-100">
        <h1 className="text-3xl font-bold text-slate-800 mb-2">Java Swing 기반 멀티플레이어 FPS (SwingFpsGame)</h1>
        <p className="text-slate-600 max-w-3xl">
          순수 Java Swing/AWT로 개발된 2D 탑다운 슈팅 게임입니다. 
          방대한 GamePanel을 8개의 Manager 클래스로 리팩토링하여 MVC 패턴 기반의 견고하고 확장 가능한 아키텍처를 구축했습니다.
          10개 캐릭터(4개 활성), 24개 스킬 이펙트, 60 FPS 게임 루프를 특징으로 합니다.
        </p>
      </div>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {[
          { label: 'Max Players', value: '4인', sub: 'RED vs BLUE (2:2)', color: 'border-blue-500' },
          { label: 'Characters', value: '10개', sub: '4개 활성 + 6개 구현완료', color: 'border-emerald-500' },
          { label: 'Managers', value: '8개', sub: 'Phase 2 리팩토링', color: 'border-amber-500' },
          { label: 'Performance', value: '60 FPS', sub: '16ms Game Loop', color: 'border-indigo-500' },
        ].map((card, idx) => (
          <div key={idx} className={`bg-white p-5 rounded-lg shadow-sm border-l-4 ${card.color} transition-transform duration-200 hover:translate-y-[-2px] hover:shadow-md`}>
            <div className="text-slate-400 text-xs font-semibold uppercase">{card.label}</div>
            <div className="text-3xl font-bold text-slate-800 mt-1">{card.value}</div>
            <div className="text-xs text-slate-500 mt-1">{card.sub}</div>
          </div>
        ))}
      </div>

      {/* Project Evolution Timeline */}
      <div className="bg-gradient-to-r from-indigo-600 to-purple-600 rounded-xl shadow-lg p-6 text-white">
        <h3 className="text-xl font-bold mb-4">🚀 프로젝트 진화 과정</h3>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className="bg-white/10 p-4 rounded-lg">
            <div className="text-2xl font-bold mb-2">10월 ~ 11월</div>
            <div className="text-indigo-100 text-sm space-y-1">
              <div>• 기본 게임 시스템 구축</div>
              <div>• 로비 및 네트워크 기능</div>
              <div>• 캐릭터 스킬 구현</div>
            </div>
          </div>
          <div className="bg-white/10 p-4 rounded-lg">
            <div className="text-2xl font-bold mb-2">11월 말</div>
            <div className="text-indigo-100 text-sm space-y-1">
              <div>• UI/UX 대폭 개선</div>
              <div>• 스킬 이펙트 시스템</div>
              <div>• 코드 정리 작업</div>
            </div>
          </div>
          <div className="bg-white/10 p-4 rounded-lg">
            <div className="text-2xl font-bold mb-2">12월 초</div>
            <div className="text-indigo-100 text-sm space-y-1">
              <div>• Phase 1: 렌더링 분리</div>
              <div>• Phase 2: 상태 관리 통합</div>
              <div>• SOLID 원칙 적용</div>
            </div>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
        <div className="bg-white p-6 rounded-xl shadow-sm border border-slate-100">
          <h3 className="text-lg font-bold text-slate-800 mb-4">기술 스택 구성 (Tech Stack)</h3>
          <p className="text-sm text-slate-500 mb-6">프로젝트에 사용된 핵심 기술 요소들의 비중입니다. UI 구성을 위한 Swing과 게임 로직 처리를 위한 Core Java가 주축을 이룹니다.</p>
          <ChartComponent type="doughnut" data={chartData} options={{ plugins: { legend: { position: 'bottom' } } }} />
        </div>

        <div className="bg-white p-6 rounded-xl shadow-sm border border-slate-100">
          <h3 className="text-lg font-bold text-slate-800 mb-4">핵심 구현 기능</h3>
          <div className="space-y-4">
            {[
              { id: 1, title: 'Phase 2 리팩토링', desc: '방대한 GamePanel을 8개 Manager 클래스로 분리하여 유지보수성 개선. MVC + Manager 패턴 적용.' },
              { id: 2, title: '네트워크 동기화', desc: 'TCP Socket 기반 멀티스레드 서버(1,093줄). 16개 프로토콜 타입(JOIN, CHAT, TEAM, POS, SHOOT, HIT 등), 200ms 피격 쿨다운.' },
              { id: 3, title: '캐릭터 시스템', desc: '10개 캐릭터(4개 활성: Raven, Piper, Technician, General). 24개 스킬 이펙트 클래스로 시각 피드백 구현.' },
              { id: 4, title: '확장성', desc: 'JSON 맵 데이터(F3/F4/F5 에디터), Factory 패턴 기반 스킬 생성. 콘텐츠 추가 용이.' }
            ].map((item) => (
              <div key={item.id} className="flex items-start">
                <div className="flex-shrink-0 h-6 w-6 flex items-center justify-center rounded-full bg-blue-100 text-blue-600 font-bold text-xs">{item.id}</div>
                <div className="ml-4">
                  <h4 className="text-sm font-bold text-slate-800">{item.title}</h4>
                  <p className="text-sm text-slate-600">{item.desc}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Key Metrics */}
      <div className="bg-white rounded-xl shadow-sm p-6 border border-slate-100">
        <h3 className="text-lg font-bold text-slate-800 mb-6">📊 프로젝트 통계</h3>
        <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
          <div className="text-center p-4 bg-slate-50 rounded-lg">
            <div className="text-3xl font-bold text-slate-800">51</div>
            <div className="text-sm text-slate-600 mt-1">Java 파일</div>
          </div>
          <div className="text-center p-4 bg-slate-50 rounded-lg">
            <div className="text-3xl font-bold text-slate-800">15K+</div>
            <div className="text-sm text-slate-600 mt-1">코드 라인</div>
          </div>
          <div className="text-center p-4 bg-slate-50 rounded-lg">
            <div className="text-3xl font-bold text-slate-800">27</div>
            <div className="text-sm text-slate-600 mt-1">프로토콜</div>
          </div>
          <div className="text-center p-4 bg-slate-50 rounded-lg">
            <div className="text-3xl font-bold text-slate-800">22</div>
            <div className="text-sm text-slate-600 mt-1">이펙트 클래스</div>
          </div>
          <div className="text-center p-4 bg-slate-50 rounded-lg">
            <div className="text-3xl font-bold text-slate-800">800</div>
            <div className="text-sm text-slate-600 mt-1">코드 감소(줄)</div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default OverviewSection;
