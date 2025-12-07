import React, { useState, useEffect } from 'react';
import { 
  Monitor, 
  Server, 
  Shield, 
  Activity, 
  Wrench, 
  Menu
} from 'lucide-react';

// Import components
import OverviewSection from './components/OverviewSection';
import ArchitectureSection from './components/ArchitectureSection';
import TechStackSection from './components/TechStackSection';
import ArmorySection from './components/ArmorySection';
import NetworkSection from './components/NetworkSection';
import FeaturesSection from './components/FeaturesSection';
import CodeSection from './components/CodeSection';
import DevToolsSection from './components/DevToolsSection';
import ProjectReportSection from './components/ProjectReportSection';

const App = () => {
  const [activeTab, setActiveTab] = useState('overview');

  // Chart.js 로드를 위한 스크립트 주입
  useEffect(() => {
    if (!window.Chart) {
      const script = document.createElement('script');
      script.src = "https://cdn.jsdelivr.net/npm/chart.js";
      script.async = true;
      document.body.appendChild(script);
    }
  }, []);

  const tabs = [
    { id: 'overview', label: '개요 (Overview)', icon: Monitor },
    { id: 'architecture', label: '아키텍처 (Architecture)', icon: Server },
    { id: 'techstack', label: '기술 스택', icon: Activity },
    { id: 'armory', label: '캐릭터 분석 (Armory)', icon: Shield },
    { id: 'network', label: '네트워크 & 프로토콜', icon: Activity },
    { id: 'features', label: '주요 기능', icon: Wrench },
    { id: 'code', label: '코드 분석', icon: Activity },
    { id: 'report', label: '프로젝트 보고서', icon: Monitor },
    { id: 'devtools', label: '개발 도구', icon: Wrench }
  ];

  return (
    <div className="bg-slate-50 min-h-screen flex flex-col font-sans text-slate-800">
      <header className="bg-white shadow-sm sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">
            <div className="flex items-center">
              <span className="text-2xl font-bold text-slate-800 tracking-tight">
                NetFps <span className="text-blue-600 text-sm font-medium uppercase ml-2">Project Dashboard</span>
              </span>
            </div>
            
            {/* Desktop Nav */}
            <nav className="hidden md:flex space-x-8">
              {tabs.map(tab => (
                <button
                  key={tab.id}
                  onClick={() => setActiveTab(tab.id)}
                  className={`px-3 py-2 text-sm font-medium transition-colors duration-200 ${
                    activeTab === tab.id
                      ? 'border-b-2 border-blue-500 text-blue-700 font-bold'
                      : 'text-slate-500 hover:text-slate-700'
                  }`}
                >
                  {tab.label}
                </button>
              ))}
            </nav>

            {/* Mobile Menu Button */}
            <div className="md:hidden">
              <button className="text-slate-500 hover:text-slate-700">Menu</button>
            </div>
          </div>
        </div>
        
        {/* Mobile Nav (Simple view) */}
        <div className="md:hidden bg-white border-t border-slate-100 flex overflow-x-auto p-2 gap-2">
           {tabs.map(tab => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`flex-shrink-0 px-3 py-2 text-xs font-medium rounded-full ${
                  activeTab === tab.id
                    ? 'bg-blue-600 text-white'
                    : 'bg-slate-100 text-slate-600'
                }`}
              >
                {tab.label.split(' (')[0]}
              </button>
            ))}
        </div>
      </header>

      <main className="flex-grow max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 w-full">
        {activeTab === 'overview' && <OverviewSection />}
        {activeTab === 'architecture' && <ArchitectureSection />}
        {activeTab === 'techstack' && <TechStackSection />}
        {activeTab === 'armory' && <ArmorySection />}
        {activeTab === 'network' && <NetworkSection />}
        {activeTab === 'features' && <FeaturesSection />}
        {activeTab === 'code' && <CodeSection />}
        {activeTab === 'report' && <ProjectReportSection />}
        {activeTab === 'devtools' && <DevToolsSection />}
      </main>

      <footer className="bg-slate-800 text-slate-400 py-8 mt-12">
        <div className="max-w-7xl mx-auto px-4 text-center">
          <p className="text-sm">© NetFps Project Team. All Rights Reserved.</p>
          <p className="text-xs mt-2 text-slate-500">React Dashboard Conversion</p>
        </div>
      </footer>
    </div>
  );
};

export default App;
