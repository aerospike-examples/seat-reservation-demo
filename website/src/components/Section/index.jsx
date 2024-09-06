import styles from "./index.module.css";
import Modal from "../Modal";
import Legend from "../Legend";
import { useModal } from "../../hooks/useModal";
import ShoppingCart from "../ShoppingCart";
import SeatMap from "./SeatMap";

const Section = ({sectionName, ...rest}) => {
    const { modalProps, ref, openModal, closeModal } = useModal();
    const { updateSeats, eventID } = rest;

    return (
		<>
		<SeatMap {...rest} onClick={openModal} className={styles.seatMapSmall} />
		{modalProps.open && 
		<Modal {...modalProps} ref={ref}>
			<div className={styles.cart}>
				<div className={styles.selection}>
					<h3>Section {sectionName}</h3>
					<Legend className={styles.legend}/>
					<SeatMap {...rest} />
				</div>
				<ShoppingCart updateSeats={updateSeats} eventID={eventID} closeModal={closeModal} />
			</div>
		</Modal>}
		</>
    )
}

export default Section;