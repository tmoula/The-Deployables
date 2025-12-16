import { createContext, useContext, useState } from 'react';

const TourContext = createContext();

export const useTour = () => {
  const context = useContext(TourContext);
  if (!context) {
    throw new Error('useTour must be used within TourProvider');
  }
  return context;
};

export const TourProvider = ({ children }) => {
  const [runTour, setRunTour] = useState(false);

  const startTour = () => {
    setRunTour(true);
  };

  const stopTour = () => {
    setRunTour(false);
  };

  return (
    <TourContext.Provider value={{ runTour, startTour, stopTour }}>
      {children}
    </TourContext.Provider>
  );
};

