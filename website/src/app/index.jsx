import Footer from '../components/Footer';
import styles from './index.module.css';
import { Outlet } from 'react-router-dom';

const App = () => {
  return (
    <div className={styles.container}>
      <div className={styles.contentWrapper}>
        <Outlet />
        <Footer />
      </div>
    </div>
  )
}

export default App
