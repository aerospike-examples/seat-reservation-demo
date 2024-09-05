import styles from "./index.module.css";

const Footer = () => {
    return (
        <>
        <hr className={styles.footerDivider} />
        <div className={styles.footer}>
          Ticketfaster is not a real corporation. Any similarity to a real
          corporation is simply coincidence.
        </div>
        </>
    )
}

export default Footer;