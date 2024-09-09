import styles from "./index.module.css";
import { useCart } from "../../../hooks/useCart";
import clsx from "clsx";;
import Seat from "../Seat";

const SeatMap = ({section, rows, updateSeats, className, eventID, onClick = () => {}}) => {
  	const { addToCart, removeFromCart, getSessionCart } = useCart();
    const { seats } = getSessionCart(eventID);

	const handleSeat = (seatID, value) => {
		switch(value) {
			case 0:
				return addToCart(seatID, eventID, () => updateSeats([{seatID, value: 3}]));
			case 3:
				return removeFromCart(seatID, eventID, () => updateSeats([{seatID, value: 0}]));
			default:
				return
		}
	}
	let sectionID = section.toString();

	return (
		<div className={styles.concertSeatingSection} onClick={onClick}>
			<div className={clsx(styles.concertSeatingSectionSeats, className)} style={{gridTemplateColumns: "repeat(24, 1fr)"}}>
			{rows.map((row, idx) => {
				let rowID = `${sectionID}-${idx}`
				
				return row.map((value, seat) => {
					let seatID = `${rowID}-${seat}`
					value = (value === 1 && seats && seats.includes(seatID)) ? 3 : value;
					
					return <Seat key={seat} value={value} onClick={() => handleSeat(seatID, value)} />
				})
			})}
			</div>
		</div>
	)
}

export default SeatMap;