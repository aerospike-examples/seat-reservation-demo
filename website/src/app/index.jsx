import Footer from '../components/Footer';
import Logo from '../components/Logo';
import styles from './index.module.css';
import { Outlet } from 'react-router-dom';
import { useLocation } from 'react-router-dom';

const App = () => {
  const { state } = useLocation();
  let eventID = state?.eventID;

  return (
    <div className={styles.container}>
      <div className={styles.contentWrapper}>
        <Logo eventID={eventID} />
        <Outlet />
        <Footer />
      </div>
    </div>
  )
}

export default App
