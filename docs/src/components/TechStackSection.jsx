import React from 'react';

const TechStackSection = () => {
  return (
    <div className="space-y-8 animate-fade-in">
      <div className="bg-white rounded-xl shadow-sm p-6 border border-slate-100">
        <h2 className="text-2xl font-bold text-slate-800 mb-2">기술 스택 상세 (Tech Stack Details)</h2>
        <p className="text-slate-600">
          NetFps 프로젝트에 사용된 기술과 라이브러리, 그리고 각각의 역할을 자세히 살펴봅니다.
        </p>
      </div>

      {/* 클라이언트 기술 스택 */}
      <div className="bg-white rounded-xl shadow-sm p-6 border border-slate-100">
        <h3 className="text-xl font-bold text-slate-800 mb-4 flex items-center gap-2">
          <span className="text-2xl">💻</span>
          클라이언트 (Client)
        </h3>

        <div className="space-y-4">
          <div className="border-l-4 border-blue-500 pl-4">
            <h4 className="font-bold text-slate-800 mb-2">Java Swing / AWT</h4>
            <p className="text-sm text-slate-600 mb-2">
              순수 Java로 구현된 GUI 프레임워크입니다. 크로스 플랫폼을 지원하며 별도의 라이브러리 설치가 필요 없습니다.
            </p>
            <div className="bg-slate-50 p-3 rounded text-xs font-mono">
              <div className="text-slate-600">// JFrame으로 게임 윈도우 생성</div>
              <div className="text-slate-800">JFrame frame = new JFrame("NetFps");</div>
              <div className="text-slate-800">frame.setSize(1280, 720);</div>
              <div className="text-slate-800">frame.add(new GamePanel());</div>
            </div>
            <div className="mt-2 flex gap-2">
              <span className="px-2 py-1 bg-blue-100 text-blue-700 rounded text-xs">GUI</span>
              <span className="px-2 py-1 bg-blue-100 text-blue-700 rounded text-xs">Cross-platform</span>
              <span className="px-2 py-1 bg-blue-100 text-blue-700 rounded text-xs">Built-in</span>
            </div>
          </div>

          <div className="border-l-4 border-emerald-500 pl-4">
            <h4 className="font-bold text-slate-800 mb-2">Graphics2D API</h4>
            <p className="text-sm text-slate-600 mb-2">
              2D 그래픽 렌더링을 위한 Java API입니다. 이미지, 도형, 텍스트를 화면에 그릴 수 있습니다.
            </p>
            <div className="bg-slate-50 p-3 rounded text-xs font-mono">
              <div className="text-slate-600">// 안티앨리어싱 적용</div>
              <div className="text-slate-800">g2d.setRenderingHint(</div>
              <div className="text-slate-800 ml-4">RenderingHints.KEY_ANTIALIASING,</div>
              <div className="text-slate-800 ml-4">RenderingHints.VALUE_ANTIALIAS_ON</div>
              <div className="text-slate-800">);</div>
            </div>
            <div className="mt-2 flex gap-2">
              <span className="px-2 py-1 bg-emerald-100 text-emerald-700 rounded text-xs">2D Rendering</span>
              <span className="px-2 py-1 bg-emerald-100 text-emerald-700 rounded text-xs">Anti-aliasing</span>
            </div>
          </div>

          <div className="border-l-4 border-amber-500 pl-4">
            <h4 className="font-bold text-slate-800 mb-2">BufferedImage</h4>
            <p className="text-sm text-slate-600 mb-2">
              이미지를 메모리에 캐싱하여 반복적인 로딩을 방지합니다. 성능 향상에 중요한 역할을 합니다.
            </p>
            <div className="bg-slate-50 p-3 rounded text-xs font-mono">
              <div className="text-slate-600">// 이미지 캐싱</div>
              <div className="text-slate-800">Map{'<'}String, BufferedImage{'>'} cache = new HashMap{'<'}{'>'};()</div>
              <div className="text-slate-800">BufferedImage img = ImageIO.read(new File("icon.png"));</div>
              <div className="text-slate-800">cache.put("raven", img);</div>
            </div>
            <div className="mt-2 flex gap-2">
              <span className="px-2 py-1 bg-amber-100 text-amber-700 rounded text-xs">Image Caching</span>
              <span className="px-2 py-1 bg-amber-100 text-amber-700 rounded text-xs">Performance</span>
            </div>
          </div>
        </div>
      </div>

      {/* 서버 기술 스택 */}
      <div className="bg-white rounded-xl shadow-sm p-6 border border-slate-100">
        <h3 className="text-xl font-bold text-slate-800 mb-4 flex items-center gap-2">
          <span className="text-2xl">🖥️</span>
          서버 (Server)
        </h3>

        <div className="space-y-4">
          <div className="border-l-4 border-purple-500 pl-4">
            <h4 className="font-bold text-slate-800 mb-2">Java Socket (TCP)</h4>
            <p className="text-sm text-slate-600 mb-2">
              신뢰성 있는 TCP 연결로 클라이언트와 서버 간 통신을 담당합니다. 순서 보장과 에러 체크를 제공합니다.
            </p>
            <div className="bg-slate-50 p-3 rounded text-xs font-mono">
              <div className="text-slate-600">// 서버 소켓 생성</div>
              <div className="text-slate-800">ServerSocket serverSocket = new ServerSocket(7777);</div>
              <div className="text-slate-800">Socket client = serverSocket.accept();</div>
              <div className="text-slate-600 mt-1">// 데이터 송수신</div>
              <div className="text-slate-800">PrintWriter out = new PrintWriter(client.getOutputStream());</div>
              <div className="text-slate-800">BufferedReader in = new BufferedReader(</div>
              <div className="text-slate-800 ml-4">new InputStreamReader(client.getInputStream())</div>
              <div className="text-slate-800">);</div>
            </div>
            <div className="mt-2 flex gap-2">
              <span className="px-2 py-1 bg-purple-100 text-purple-700 rounded text-xs">TCP</span>
              <span className="px-2 py-1 bg-purple-100 text-purple-700 rounded text-xs">Reliable</span>
              <span className="px-2 py-1 bg-purple-100 text-purple-700 rounded text-xs">Port 7777</span>
            </div>
          </div>

          <div className="border-l-4 border-indigo-500 pl-4">
            <h4 className="font-bold text-slate-800 mb-2">Multi-threading</h4>
            <p className="text-sm text-slate-600 mb-2">
              각 클라이언트마다 독립적인 스레드를 할당하여 병렬 처리를 구현합니다. 동시 접속자 처리가 가능합니다.
            </p>
            <div className="bg-slate-50 p-3 rounded text-xs font-mono">
              <div className="text-slate-600">// 클라이언트 핸들러 스레드</div>
              <div className="text-slate-800">Thread clientThread = new Thread(() -{'>'} {'{'}</div>
              <div className="text-slate-800 ml-4">// 클라이언트 처리 로직</div>
              <div className="text-slate-800 ml-4">handleClient(socket);</div>
              <div className="text-slate-800">{'}'}); </div>
              <div className="text-slate-800">clientThread.start();</div>
            </div>
            <div className="mt-2 flex gap-2">
              <span className="px-2 py-1 bg-indigo-100 text-indigo-700 rounded text-xs">Concurrent</span>
              <span className="px-2 py-1 bg-indigo-100 text-indigo-700 rounded text-xs">Scalable</span>
            </div>
          </div>

          <div className="border-l-4 border-pink-500 pl-4">
            <h4 className="font-bold text-slate-800 mb-2">Custom Protocol</h4>
            <p className="text-sm text-slate-600 mb-2">
              텍스트 기반의 경량 프로토콜입니다. 파싱이 간단하고 디버깅이 쉬우며 확장성이 좋습니다.
            </p>
            <div className="bg-slate-50 p-3 rounded text-xs font-mono">
              <div className="text-slate-600">// 프로토콜 예시</div>
              <div className="text-slate-800">MOVE:150.5,200.3</div>
              <div className="text-slate-800">SHOOT:500,400,45,100</div>
              <div className="text-slate-800">HITME:Alice</div>
              <div className="text-slate-800">CHAT:Hello World</div>
              <div className="text-slate-800">SKILL:2,raven</div>
            </div>
            <div className="mt-2 flex gap-2">
              <span className="px-2 py-1 bg-pink-100 text-pink-700 rounded text-xs">Text-based</span>
              <span className="px-2 py-1 bg-pink-100 text-pink-700 rounded text-xs">Lightweight</span>
              <span className="px-2 py-1 bg-pink-100 text-pink-700 rounded text-xs">Extensible</span>
            </div>
          </div>
        </div>
      </div>

      {/* 데이터 처리 */}
      <div className="bg-white rounded-xl shadow-sm p-6 border border-slate-100">
        <h3 className="text-xl font-bold text-slate-800 mb-4 flex items-center gap-2">
          <span className="text-2xl">📦</span>
          데이터 처리 (Data)
        </h3>

        <div className="space-y-4">
          <div className="border-l-4 border-cyan-500 pl-4">
            <h4 className="font-bold text-slate-800 mb-2">JSON (org.json)</h4>
            <p className="text-sm text-slate-600 mb-2">
              맵 데이터와 설정 파일을 저장하는 포맷입니다. 가독성이 좋고 파싱이 간단합니다.
            </p>
            <div className="bg-slate-50 p-3 rounded text-xs font-mono">
              <div className="text-slate-800">{`{`}</div>
              <div className="text-slate-800 ml-2">"width": 50,</div>
              <div className="text-slate-800 ml-2">"height": 30,</div>
              <div className="text-slate-800 ml-2">"tiles": [</div>
              <div className="text-slate-800 ml-4">[0, 0, 1, 1, 0],</div>
              <div className="text-slate-800 ml-4">[0, 0, 0, 0, 0]</div>
              <div className="text-slate-800 ml-2">]</div>
              <div className="text-slate-800">{`}`}</div>
            </div>
            <div className="mt-2 flex gap-2">
              <span className="px-2 py-1 bg-cyan-100 text-cyan-700 rounded text-xs">Map Data</span>
              <span className="px-2 py-1 bg-cyan-100 text-cyan-700 rounded text-xs">Human-readable</span>
            </div>
          </div>

          <div className="border-l-4 border-teal-500 pl-4">
            <h4 className="font-bold text-slate-800 mb-2">HashMap / ArrayList</h4>
            <p className="text-sm text-slate-600 mb-2">
              Java Collections Framework를 활용한 효율적인 데이터 구조입니다.
            </p>
            <div className="bg-slate-50 p-3 rounded text-xs font-mono">
              <div className="text-slate-600">// 플레이어 관리</div>
              <div className="text-slate-800">Map{'<'}String, Player{'>'} players = new HashMap{'<'}{'>'};();</div>
              <div className="text-slate-800">players.put("Alice", new Player("Alice"));</div>
              <div className="text-slate-600 mt-1">// 미사일 관리</div>
              <div className="text-slate-800">List{'<'}Missile{'>'} missiles = new ArrayList{'<'}{'>'};();</div>
              <div className="text-slate-800">missiles.add(new Missile(x, y, angle));</div>
            </div>
            <div className="mt-2 flex gap-2">
              <span className="px-2 py-1 bg-teal-100 text-teal-700 rounded text-xs">O(1) Access</span>
              <span className="px-2 py-1 bg-teal-100 text-teal-700 rounded text-xs">Dynamic</span>
            </div>
          </div>
        </div>
      </div>

      {/* 개발 도구 */}
      <div className="bg-white rounded-xl shadow-sm p-6 border border-slate-100">
        <h3 className="text-xl font-bold text-slate-800 mb-4 flex items-center gap-2">
          <span className="text-2xl">🛠️</span>
          개발 환경 (Development)
        </h3>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="p-4 bg-slate-50 rounded-lg border border-slate-200">
            <div className="flex items-center gap-2 mb-2">
              <span className="text-2xl">☕</span>
              <h4 className="font-bold text-slate-800">Java 17+</h4>
            </div>
            <p className="text-sm text-slate-600">
              최신 LTS 버전의 Java를 사용합니다. Record, Pattern Matching 등 모던 기능 활용.
            </p>
          </div>

          <div className="p-4 bg-slate-50 rounded-lg border border-slate-200">
            <div className="flex items-center gap-2 mb-2">
              <span className="text-2xl">🔨</span>
              <h4 className="font-bold text-slate-800">Eclipse IDE</h4>
            </div>
            <p className="text-sm text-slate-600">
              Java 개발을 위한 통합 개발 환경입니다. 디버깅과 리팩토링 도구 제공.
            </p>
          </div>

          <div className="p-4 bg-slate-50 rounded-lg border border-slate-200">
            <div className="flex items-center gap-2 mb-2">
              <span className="text-2xl">📚</span>
              <h4 className="font-bold text-slate-800">No External Dependencies</h4>
            </div>
            <p className="text-sm text-slate-600">
              외부 라이브러리 최소화. org.json만 사용하여 의존성 관리 간소화.
            </p>
          </div>

          <div className="p-4 bg-slate-50 rounded-lg border border-slate-200">
            <div className="flex items-center gap-2 mb-2">
              <span className="text-2xl">🎮</span>
              <h4 className="font-bold text-slate-800">자체 엔진</h4>
            </div>
            <p className="text-sm text-slate-600">
              Unity, LibGDX 등 기존 엔진 없이 순수 Java로 구현한 2D 게임 엔진.
            </p>
          </div>
        </div>
      </div>

      {/* 아키텍처 패턴 */}
      <div className="bg-white rounded-xl shadow-sm p-6 border border-slate-100">
        <h3 className="text-xl font-bold text-slate-800 mb-4 flex items-center gap-2">
          <span className="text-2xl">🏗️</span>
          디자인 패턴 (Design Patterns)
        </h3>

        <div className="space-y-4">
          <div className="p-4 bg-blue-50 rounded-lg border-l-4 border-blue-500">
            <h4 className="font-bold text-blue-800 mb-2">MVC (Model-View-Controller)</h4>
            <ul className="text-sm text-slate-600 space-y-1">
              <li><strong>Model:</strong> GameState - 게임 상태 데이터</li>
              <li><strong>View:</strong> GameRenderer - 화면 렌더링</li>
              <li><strong>Controller:</strong> GamePanel - 입력 처리 및 로직 제어</li>
            </ul>
          </div>

          <div className="p-4 bg-emerald-50 rounded-lg border-l-4 border-emerald-500">
            <h4 className="font-bold text-emerald-800 mb-2">Manager Pattern</h4>
            <ul className="text-sm text-slate-600 space-y-1">
              <li><strong>MapManager:</strong> 맵 데이터 로드 및 충돌 검사</li>
              <li><strong>SkillManager:</strong> 스킬 쿨다운 및 효과 관리</li>
              <li><strong>CollisionManager:</strong> 충돌 감지 및 처리</li>
              <li><strong>ObjectManager:</strong> 게임 오브젝트 생성/소멸</li>
            </ul>
          </div>

          <div className="p-4 bg-purple-50 rounded-lg border-l-4 border-purple-500">
            <h4 className="font-bold text-purple-800 mb-2">Observer Pattern</h4>
            <p className="text-sm text-slate-600">
              서버가 게임 이벤트를 브로드캐스트하면 모든 클라이언트가 업데이트를 수신합니다.
            </p>
          </div>

          <div className="p-4 bg-amber-50 rounded-lg border-l-4 border-amber-500">
            <h4 className="font-bold text-amber-800 mb-2">Factory Pattern</h4>
            <p className="text-sm text-slate-600">
              캐릭터와 스킬 객체 생성 시 팩토리 패턴을 사용하여 코드 재사용성을 높였습니다.
            </p>
          </div>
        </div>
      </div>

      {/* 프로젝트 구조 */}
      <div className="bg-white rounded-xl shadow-sm p-6 border border-slate-100">
        <h3 className="text-xl font-bold text-slate-800 mb-4">프로젝트 구조</h3>
        
        <div className="bg-slate-900 p-4 rounded-lg font-mono text-sm text-slate-100">
          <div>SwingFpsGame/</div>
          <div className="ml-4">├── src/com/fpsgame/</div>
          <div className="ml-8">│   ├── client/</div>
          <div className="ml-12 text-emerald-400">│   │   ├── GamePanel.java (2,413줄 - 현재)</div>
          <div className="ml-12 text-emerald-400">│   │   ├── GameState.java (437줄)</div>
          <div className="ml-12 text-emerald-400">│   │   ├── GameRenderer.java (785줄)</div>
          <div className="ml-12 text-emerald-400">│   │   ├── NetworkClient.java (150줄)</div>
          <div className="ml-12 text-emerald-400">│   │   ├── MapManager.java (686줄)</div>
          <div className="ml-12 text-emerald-400">│   │   ├── SkillManager.java (320줄)</div>
          <div className="ml-12 text-emerald-400">│   │   ├── UIManager.java (240줄)</div>
          <div className="ml-12 text-emerald-400">│   │   ├── CollisionManager.java (153줄)</div>
          <div className="ml-12 text-emerald-400">│   │   ├── GameObjectManager.java (338줄)</div>
          <div className="ml-12 text-emerald-400">│   │   ├── GameLogicController.java (280줄)</div>
          <div className="ml-12 text-emerald-400">│   │   ├── PlayerMovementController.java (195줄)</div>
          <div className="ml-12 text-emerald-400">│   │   └── SpawnManager.java (~150줄)</div>
          <div className="ml-8">│   ├── server/</div>
          <div className="ml-12 text-blue-400">│   │   ├── GameServer.java (1,093줄)</div>
          <div className="ml-12 text-blue-400">│   │   └── ClientHandler.java</div>
          <div className="ml-8">│   ├── common/</div>
          <div className="ml-12 text-amber-400">│   │   ├── CharacterData.java (291줄, 10 캐릭터)</div>
          <div className="ml-12 text-amber-400">│   │   ├── Ability.java (스킬 모델)</div>
          <div className="ml-12 text-amber-400">│   │   ├── PlayerData.java</div>
          <div className="ml-12 text-amber-400">│   │   ├── Missile.java</div>
          <div className="ml-12 text-amber-400">│   │   └── Obstacle.java</div>
          <div className="ml-8">│   └── effects/ (24개 스킬 이펙트 클래스)</div>
          <div className="ml-12 text-purple-400">│       ├── RavenDashEffect.java</div>
          <div className="ml-12 text-purple-400">│       ├── PiperMarkEffect.java</div>
          <div className="ml-12 text-purple-400">│       ├── GeneralAuraEffect.java</div>
          <div className="ml-12 text-purple-400">│       └── ... (19개 더)</div>
          <div className="ml-4">├── resources/</div>
          <div className="ml-8 text-cyan-400">│   ├── maps/ (map.json, map2.json, village.json...)</div>
          <div className="ml-8 text-cyan-400">│   └── icons/ (캐릭터 아이콘 10개)</div>
          <div className="ml-4">├── lib/</div>
          <div className="ml-8">│   └── org.json.jar</div>
          <div className="ml-4">└── code-review/ (코드 리뷰 문서)</div>
          <div className="ml-8 text-slate-400">    ├── GamePanel_Review.md</div>
          <div className="ml-8 text-slate-400">    ├── CharacterData_Review.md</div>
          <div className="ml-8 text-slate-400">    └── Manager_Classes_Review.md</div>
        </div>

        <div className="mt-4 p-4 bg-blue-50 rounded-lg border-l-4 border-blue-500">
          <h4 className="font-bold text-slate-800 mb-2">📊 프로젝트 통계</h4>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-3 text-sm">
            <div className="bg-white p-3 rounded text-center">
              <div className="text-2xl font-bold text-blue-600">~8,000+</div>
              <div className="text-xs text-slate-600">총 코드 라인</div>
            </div>
            <div className="bg-white p-3 rounded text-center">
              <div className="text-2xl font-bold text-emerald-600">8개</div>
              <div className="text-xs text-slate-600">Manager 클래스</div>
            </div>
            <div className="bg-white p-3 rounded text-center">
              <div className="text-2xl font-bold text-purple-600">10개</div>
              <div className="text-xs text-slate-600">캐릭터 (4개 활성)</div>
            </div>
            <div className="bg-white p-3 rounded text-center">
              <div className="text-2xl font-bold text-amber-600">24개</div>
              <div className="text-xs text-slate-600">스킬 이펙트</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default TechStackSection;
