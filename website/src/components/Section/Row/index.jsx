import Seat from "../Seat";

const Row = ({row, seatID, handleSeat}) => {
    return (
      row.map((value, seat) => {
        let seatIDFinal = `${seatID}-${seat}`
        return (
          <Seat value={value} key={seat} seatID={seatIDFinal} handleSeat={handleSeat} />
        )
      })
    )
}

export default Row;