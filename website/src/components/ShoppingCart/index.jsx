import { useCart } from "../../hooks/useCart";
import clsx from "clsx";
import styles from "./index.module.css";

const ShoppingCart = ({updateSeats, eventID, closeModal}) => {
    const { cartItems, total, emptyCart, purchaseCart } =  useCart();
    const formatItems = (value) => {
        let formattedItems = []
        for(let item of cartItems) {
            formattedItems.push({seatID: item, value})
        }
        return formattedItems;
    }
    const abandon = () => {
        let abandonItems = formatItems(0);
        emptyCart(eventID, () => updateSeats(abandonItems));
    }

    const purchase = () => {
        let purchaseItems = formatItems(2);
        purchaseCart(eventID, () => {
            updateSeats(purchaseItems)
            closeModal();
        })
    }

    return (
        <div className={styles.cartItems}>
            <h3>Shopping Cart</h3>
            {cartItems.map(item => {
              const [secNum, rowNum, seatNum] = item.split("-");
              let section = String.fromCharCode(parseInt(secNum) + 65);
              return (
              <div className={styles.item} key={item}>
                <span>Sec: {section} Row: {parseInt(rowNum) + 1} Seat: {parseInt(seatNum) + 1}</span>
                <span>$150.00</span>
              </div>
              )
            })}
            <div className={styles.shoppingCartTotal}>
              Total: $<span className={styles.shoppingCartTotalAmount}>{total}.00</span>
            </div>
            <div className={styles.shoppingCartActions}>
                <button className={clsx(styles.shoppingCartButton, styles.checkout)} onClick={purchase} disabled={cartItems.length < 1}>Submit Payment</button>
                <button className={clsx(styles.shoppingCartButton, styles.abandonCart)} onClick={abandon}>Abandon Cart</button>
            </div>
        </div>
    )
}

export default ShoppingCart;