import { createContext, useEffect, useRef, useState } from "react";

export const CartContext = createContext(null);

// eslint-disable-next-line react/prop-types
const CartProvider = ({children}) => {
  const cartName = "ticket-faster-cart";
  const cartID = useRef();
  const [cartItems, setCartItems] = useState([]);
  const [total, setTotal] = useState(0);

  const createSessionCart = () => {
    cartID.current = Math.floor(Math.random() * (Math.random() * 100000000000)).toString();
    let shoppingCart = {id: cartID.current, carts: {}};
    sessionStorage.setItem(cartName, JSON.stringify(shoppingCart));
    return shoppingCart;
  }

  const getSessionCart = () => {
    let shoppingCart = sessionStorage.getItem(cartName);
    if(!shoppingCart) return createSessionCart();

    shoppingCart = JSON.parse(shoppingCart);
    cartID.current = shoppingCart.id;
    return shoppingCart;
  }

  const setSessionCart = (newCart, eventID) => {
    let shoppingCart = getSessionCart();
    let { carts } = shoppingCart;
    carts[eventID] = newCart;
    sessionStorage.setItem(cartName, JSON.stringify(shoppingCart))
  }

  const getEventCart = (eventID) => {
    let shoppingCart = getSessionCart();
    let { carts } = shoppingCart;
    if(carts[eventID]) setCartItems(carts[eventID]);
    else setCartItems([]);
  }

  const addToCart = async (seatID, eventID, callback = () => {}) => {
    const apiUrl = import.meta.env.VITE_APP_API_URL;
    let path;
    let shoppingCart = getSessionCart();
    let { carts } = shoppingCart;
    if(carts[eventID]) path = `${apiUrl}/concerts/${eventID}/shopping-carts/${cartID.current + eventID}/seats`;
    else path = `${apiUrl}/concerts/${eventID}/shopping-carts`;
    
    let response = await fetch(path, {
      headers: {
        "Content-Type": "application/json"
      },
      method: 'POST',
      body: JSON.stringify({id: cartID.current + eventID, seats: [seatID]})
    })
    if(response.ok){
      let newCart = [...cartItems];
      newCart.push(seatID);
      setCartItems(newCart);
      setSessionCart(newCart, eventID);
      callback();
    }
  }

  const removeFromCart = async (seatID, eventID, callback) => {
    const apiUrl = import.meta.env.VITE_APP_API_URL;
    let response = await fetch(`${apiUrl}/concerts/${eventID}/shopping-carts/${cartID.current + eventID}/seats`, {
      headers: {
        "Content-Type": "application/json"
      },
      method: 'DELETE',
      body: JSON.stringify({id: cartID.current + eventID, seats: [seatID]})
    })

    if(response.ok){
      let newCart = [...cartItems]
      newCart.splice(newCart.indexOf(seatID), 1);
      setCartItems(newCart);
      setSessionCart(newCart, eventID)
      callback();
    }
  }

  const emptyCart = async (eventID, callback) => {
    const apiUrl = import.meta.env.VITE_APP_API_URL;
    let response = await fetch(`${apiUrl}/concerts/${eventID}/shopping-carts/${cartID.current + eventID}`, {
      method: 'DELETE'
    })
    if(response.ok) {
      setSessionCart([], eventID);
      setCartItems([]);
      callback();
    } 
  }

  const purchaseCart = async (eventID, callback) => {
    const apiUrl = import.meta.env.VITE_APP_API_URL;
    let response = await fetch(`${apiUrl}/concerts/${eventID}/purchases`, {
      headers: {
        "Content-Type": "application/json"
      },
      method: 'POST',
      body: JSON.stringify({id: cartID.current + eventID})
    })
    if(response.ok) {
      setSessionCart([], eventID)
      setCartItems([]);
      callback();
    }
  }

  useEffect(() => {
    setTotal(cartItems.length * 150)
  }, [cartItems]);

  useEffect(() => {
    getSessionCart();
  }, [])

  const cart = {
    cartItems,
    total,
    getEventCart,
    addToCart,
    removeFromCart,
    emptyCart,
    purchaseCart,
    getSessionCart
  }

  return (
    <CartContext.Provider value={cart} >
      {children}
    </CartContext.Provider>
  )
}

export default CartProvider;