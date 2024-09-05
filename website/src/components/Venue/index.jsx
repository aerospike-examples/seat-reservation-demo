
import { useState, useEffect, useRef } from "react"; 
import Section from "../Section";
import styles from "./index.module.css";
import Legend from "../Legend";
import { useCart } from "../../hooks/useCart";

const Venue = ({seats, eventID}) => {
    const [seatMap, setSeatMap] = useState(seats);
    const { getEventCart, getSessionCart } = useCart();
	const eventSource = useRef(null);

    const updateSeatMap = (seats) => {
      	const newSeatMap = [...seatMap];
		for(let { seatID, value } of seats) {
			const [section, row, seat] = seatID.split("-");
			newSeatMap[section][row][seat] = value;
		}
		setSeatMap(newSeatMap);
    }

    const handleMessage = (e) => {
      	let [eID, seatID, value] = e.data.split(":");
		if(eID !== eventID) return
		let { cart } = getSessionCart();
		if(cart[eventID].includes(seatID)) value = 3;
		updateSeatMap([{seatID, value}]);
   	}

   	useEffect(() => {
    	getEventCart(eventID);

       	if(!eventSource.current) {
			eventSource.current = new EventSource("/concerts/updates");
			eventSource.current.addEventListener("statusChange", handleMessage);	
			return () => {
				eventSource.current.removeEventListener("statusChange", handleMessage)
			}
		}
   	}, [])

    return (
		<div className={styles.venue}>
			<div className={styles.concertStage}>STAGE</div>
			<div className={styles.concertSeatingSections}>
			<Legend className={styles.legend}/>
			{seatMap.map((section, idx) => (
				<Section 
				key={idx} 
				section={String.fromCharCode(idx + 65)} 
				sectionIdx={idx} 
				eventID={eventID} 
				seats={section}
				updateSeats={updateSeatMap} />
			))}
			</div>
		</div>
    )
}

export default Venue;