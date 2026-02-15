import { useState, useEffect } from 'react';

function getWindowDimensions() {
  return {
    height: window.innerHeight,
    width: window.innerWidth
  }
}

export const useWindowDimensions = () => {
  const [windowDimensions, setWindowDimensions] = useState(getWindowDimensions());

  useEffect(() => {
    function handleResize() {
      setWindowDimensions(getWindowDimensions());
    }

    // Add event listener for window resize
    window.addEventListener('resize', handleResize);
    
    handleResize();

    // Remove listener on unmount
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  return windowDimensions;
}

export default useWindowDimensions;