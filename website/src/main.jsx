import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import Routes from './routes.jsx';
import './css/index.css';
import CartProvider from './components/Cart/index.jsx';

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <CartProvider>
      <Routes />
    </CartProvider>
  </StrictMode>,
)
