import { createContext, useEffect, useState } from "react";
import { v4 as uuidv4 } from 'uuid';

export const CartContext = createContext(null);

const CartProvider = ({children}) => {
	const [cartItems, setCartItems] = useState([]);
	const [total, setTotal] = useState(0);

	const createSessionCart = (eventID) => {
		let shoppingCart = {
			cartID: uuidv4(),
			seats: [],
			newCart: true
		};
		sessionStorage.setItem(eventID, JSON.stringify(shoppingCart));
		return shoppingCart;
	}

	const getSessionCart = (eventID) => {
		let shoppingCart = sessionStorage.getItem(eventID);
		if(!shoppingCart) return createSessionCart(eventID);

		shoppingCart = JSON.parse(shoppingCart);
		return shoppingCart;
	}

	const setSessionCart = (cart, eventID) => {
		let { cartID } = getSessionCart(eventID);
		let seats = [...cart];
		let newCart = false;
		sessionStorage.setItem(eventID, JSON.stringify({cartID, seats, newCart}))
	}

	const removeSessionCart = (eventID) => {
		sessionStorage.removeItem(eventID);
		setCartItems([]);
	}

	const getEventCart = async (eventID) => {
		let { cartID } = getSessionCart(eventID);
		fetch(`/concerts/${eventID}/shopping-carts/${cartID}`)
		.then(response => {
			if(response.ok) {
				return response.json()
			}
			throw new Error('Cart not found');
		})
		.then(({ seats }) => {
			setSessionCart(seats, eventID);
			setCartItems(seats);
		})
		.catch((err) => {
			console.error(err);
			setCartItems([]);
		});
	}

	const addToCart = async (seatID, eventID, callback = () => {}) => {
		let path;
		let { cartID, newCart } = getSessionCart(eventID);
		let cart = [...cartItems];
		cart.push(seatID);
		setSessionCart(cart, eventID);

		if(newCart) path = `/concerts/${eventID}/shopping-carts`;
		else path = `/concerts/${eventID}/shopping-carts/${cartID}/seats`;
		
		let response = await fetch(path, {
			headers: {
				"Content-Type": "application/json"
			},
			method: 'POST',
			body: JSON.stringify({id: cartID, seats: [seatID]})
		})
		if(response.ok){
			setCartItems(cart);
			callback();
		}
		else {
			console.log(response)
			setSessionCart(cartItems, eventID)
		};
	}

	const removeFromCart = async (seatID, eventID, callback) => {
		let { cartID } = getSessionCart(eventID);
		let response = await fetch(`/concerts/${eventID}/shopping-carts/${cartID}/seats`, {
			headers: {
				"Content-Type": "application/json"
			},
			method: 'DELETE',
			body: JSON.stringify({id: cartID, seats: [seatID]})
		})

		if(response.ok){
			let cart = [...cartItems]
			cart.splice(cart.indexOf(seatID), 1);
			setCartItems(cart);
			setSessionCart(cart, eventID)
			callback();
		}
	}

	const emptyCart = async (eventID, callback) => {
		let { cartID } = getSessionCart(eventID);
		let response = await fetch(`/concerts/${eventID}/shopping-carts/${cartID}`, {
			method: 'DELETE'
		})
		if(response.ok) {
			setSessionCart([], eventID);
			setCartItems([]);
			callback();
		} 
	}

	const purchaseCart = async (eventID, callback) => {
		let { cartID } = getSessionCart(eventID);
		let response = await fetch(`/concerts/${eventID}/purchases`, {
			headers: {
				"Content-Type": "application/json"
			},
			method: 'POST',
			body: JSON.stringify({id: cartID})
		})
		if(response.ok) {
			removeSessionCart(eventID)
			callback();
		}
	}

	useEffect(() => {
		setTotal(cartItems.length * 150)
	}, [cartItems]);

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