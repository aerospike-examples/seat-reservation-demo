import styles from "./index.module.css";
import seatStyles from "../Section/Seat/index.module.css";
import clsx from "clsx";

const Legend = ({className}) => {
    return (
        <div className={clsx(styles.concertSeatingLegend, styles.legend, className)}>
            <div className={clsx(styles.concertLegendItem, styles.legendItem, seatStyles.seatAvailable)}>
                <svg viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg" width="20" height="20">
                    <path d="M10,30 L10,90 L90,90 L90,30 L80,30 L80,10 L20,10 L20,30 Z" />
                </svg>
                <span>Available</span>
            </div>
            <div className={clsx(styles.concertLegendItem, styles.legendItem, seatStyles.seatInYourCart)}>
                <svg viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg" width="20" height="20">
                    <path d="M10,30 L10,90 L90,90 L90,30 L80,30 L80,10 L20,10 L20,30 Z" />
                </svg>
                <span>Your Cart</span>
            </div>
            <div className={clsx(styles.concertLegendItem, styles.legendItem, seatStyles.seatInCart)}>
                <svg viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg" width="20" height="20">
                    <path d="M10,30 L10,90 L90,90 L90,30 L80,30 L80,10 L20,10 L20,30 Z" />
                </svg>
                <span>In Other Carts</span>
            </div>
            <div className={clsx(styles.concertLegendItem, styles.legendItem, seatStyles.seatSold)}>
                <svg viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg" width="20" height="20">
                    <path d="M10,30 L10,90 L90,90 L90,30 L80,30 L80,10 L20,10 L20,30 Z" />
                </svg>
                <span>Sold</span>
            </div>
        </div>
    )
}

export default Legend;