import { useState, useEffect, useRef } from "react"; 
import Section from "../Section";
import styles from "./index.module.css";
import Legend from "../Legend";
import { useCart } from "../../hooks/useCart";

const Venue = ({sections, eventID}) => {
    const [seatMap, setSeatMap] = useState(sections);
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
      	let [seatID, value] = e.data.split(":");
		let { seats } = getSessionCart(eventID);
		
		if(seats.includes(seatID)) return;
		updateSeatMap([{seatID, value: parseInt(value)}]);
   	}

   	useEffect(() => {
		getEventCart(eventID);
		eventSource.current = new EventSource("/concerts/updates");
		eventSource.current.addEventListener(eventID, handleMessage);	
		return () => {
			eventSource.current.removeEventListener(eventID, handleMessage)
		}
   	}, [])

    return (
		<div className={styles.venue}>
			<div className={styles.concertStage}>STAGE</div>
			<Legend className={styles.legend}/>
			<div className={styles.concertSeatingSections}>
			{seatMap.map((rows, section) => (
				<Section 
					key={section} 
					sectionName={String.fromCharCode(section + 65)} 
					section={section} 
					eventID={eventID} 
					rows={rows}
					updateSeats={updateSeatMap} />
			))}
			</div>
		</div>
    )
}

export default Venue;