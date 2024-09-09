import styles from "./index.module.css";
import clsx from "clsx";

const Seat = ({value, onClick}) => {
    const seatStatus = {
      0: "seatAvailable",
      1: "seatInCart",
      2: "seatSold",
      3: "seatInYourCart"
    }
    return (
      <div className={clsx(styles.seat, styles[seatStatus[value]])} onClick={onClick}>
        <svg viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">
            <path d="M10,30 L10,90 L90,90 L90,30 L80,30 L80,10 L20,10 L20,30 Z"></path>
            <path d="M15,35 L85,35 L85,85 L15,85 Z" fill="#FFFFFF" fillOpacity="0.2"></path>
            <path d="M25,15 L75,15 L75,30 L25,30 Z" fill="#FFFFFF" fillOpacity="0.2"></path>
        </svg>
      </div>
    )
}

export default Seat;