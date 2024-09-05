import styles from "./index.module.css";
import clsx from "clsx";;
import Row from "../Row";

const SeatMap = ({section, seats, updateSeats, onClick = () => {}, className, addToCart, removeFromCart}) => {
  const handleSeat = (seatID, value) => {
      switch(value) {
        case 0:
          return addToCart(seatID, () => updateSeats([{seatID, value: 3}]));
        case 3:
          return removeFromCart(seatID, () => updateSeats([{seatID, value: 0}]));
        default:
          return
      }
  }
  let seatID = section.toString();

  return (
    <div className={styles.concertSeatingSection} onClick={onClick}>
      <div className={clsx(styles.concertSeatingSectionSeats, className)} style={{gridTemplateColumns: "repeat(24, 1fr)"}}>
        {seats.map((row, idx) => (
          <Row row={row} seatID={`${seatID}-${idx}`} handleSeat={handleSeat} key={idx} />
        ))}
      </div>
    </div>
  )
}

export default SeatMap;