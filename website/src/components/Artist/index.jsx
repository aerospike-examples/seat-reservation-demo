import { Link } from "react-router-dom";
import styles from "./index.module.css";

const Artist = ({artist, shows}) => {
    return (
        <>
        <h2 className={styles.concertListArtistHeading}>{artist}</h2>
		{shows.map(show => {
			const { id, title, date, venue: { description } } = show;
			return (
				<div className={styles.concertListItem} key={id}>				
					<Link to={`/events/${artist}/${id}`} state={{ description, date, title, eventID: id }}><h3>{title}</h3></Link>
					<p><strong>Venue: </strong> {description}</p>
					<p><strong>Date: </strong> 
					{new Date(date).toLocaleDateString('en-US', {
						weekday: 'long',
						year: 'numeric',
						month: 'long',
						day: 'numeric',
					})}</p>
					<p><strong>Time: </strong>Doors at 19:30, Show at 20:30</p>
				</div>
			)
		})}
        </>
    )
}

export default Artist;