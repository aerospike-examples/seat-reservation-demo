import styles from "./index.module.css";
import Modal from "../Modal";
import Legend from "../Legend";
import { useModal } from "../../hooks/useModal";
import ShoppingCart from "../ShoppingCart";
import { useCart } from "../../hooks/useCart";
import SeatMap from "./SeatMap";

const Section = ({section, sectionIdx, eventID, seats, updateSeats}) => {
    const { modalProps, ref, openModal, closeModal } = useModal();
    const { addToCart, removeFromCart } = useCart();
    
    const add = (seatID, callback) => {
      addToCart(seatID, eventID, callback)
    }
    const remove = (seatID, callback) => {
      removeFromCart(seatID, eventID, callback);
    }

    return (
      <>
      <SeatMap section={sectionIdx} seats={seats} onClick={openModal} className={styles.seatMapSmall} />
      {modalProps.open && 
      <Modal
        {...modalProps}
        ref={ref}
      >
        <div className={styles.cart}>
          <div className={styles.selection}>
            <h3>Section {section}</h3>
            <Legend className={styles.legend}/>
            <SeatMap section={sectionIdx} seats={seats} addToCart={add} removeFromCart={remove} updateSeats={updateSeats}/>
          </div>
          <ShoppingCart updateSeats={updateSeats} eventID={eventID} closeModal={closeModal} />
        </div>
      </Modal>}
      </>
    )
}

export default Section;