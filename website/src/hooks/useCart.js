import { useContext } from "react";
import { CartContext } from "../components/Cart";

export const useCart = () => {
    return useContext(CartContext);
}