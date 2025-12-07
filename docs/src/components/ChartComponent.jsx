import React, { useRef, useEffect } from 'react';

/**
 * Chart.js Helper Component
 * React 환경에서 Chart.js를 안전하게 사용하기 위한 래퍼 컴포넌트입니다.
 */
const ChartComponent = ({ type, data, options, height = "300px" }) => {
  const chartRef = useRef(null);
  const chartInstance = useRef(null);

  useEffect(() => {
    // Chart.js가 로드되었는지 확인 (CDN 방식 사용 시)
    const ChartConstructor = window.Chart;
    
    if (chartRef.current && ChartConstructor) {
      if (chartInstance.current) {
        chartInstance.current.destroy();
      }

      chartInstance.current = new ChartConstructor(chartRef.current, {
        type,
        data,
        options: {
          ...options,
          responsive: true,
          maintainAspectRatio: false,
        }
      });
    }

    return () => {
      if (chartInstance.current) {
        chartInstance.current.destroy();
      }
    };
  }, [type, data, options]);

  return (
    <div className="relative w-full max-w-2xl mx-auto" style={{ height: height }}>
      <canvas ref={chartRef}></canvas>
    </div>
  );
};

export default ChartComponent;
