let cart = undefined;

$(function() {
	const $modal = $('.concert-section-modal');
	
	function getCurrentConcert() {
		// Retrieve the concert from the URL
		const url = $(location).attr('href');
		return url.substr(url.lastIndexOf('/')+1);
	}
	
	$modal.hide();
    $('.concert-seating-chart .concert-seating-section').on("click", function(evt) {
        const $section = $(evt.currentTarget);
        const sectionNumber = $section.data('sectionNumber');
		const sectionName = $section.data('sectionName');

		$modal.find('.currentSection').text(sectionName);
		
		const $modalSeatsContainer = $modal.find('.concert-section-modal-seats');
		const $source = $section.find('.concert-seating-section-seats');
		$modalSeatsContainer.empty().append($source.children().clone(true));
		
        $modal.show();
    });

	// Do not allow the click outside the modal background to propegate
	$modal.children('.concert-section-modal-content').on('click', function(evt) {
		evt.preventDefault();
		evt.stopPropagation();
	})
	
	// Hide the dialog box by clicking on the background
    $modal.on('click', function() {
        $modal.hide();
    });
	
	// Select a seat from the map of seats. Only seats that are available 
	// are able to be selected.
	$('.concert-section-modal-seats').on('click', '.seat.seat-available', function(evt) {
		const $seat = $(evt.currentTarget);
		const seatNumber = $seat.data('seat-id');
		reserveSeat(seatNumber, cart);
	})

	function updateSeatStatus(seatId, status, cartId) {
		const $seatsToUpdate = $('div.seat[data-seat-id="'+seatId+'"]');
		let newStatus;
		if (status == 1 && cartId) {
			newStatus = 'in-your-cart';
		}
		else if (status == 2) {
			newStatus = 'sold';
		}
		else if (status == 1) {
			newStatus = 'in-cart';
		}
		else {
			newStatus = 'available';
		}
		$seatsToUpdate.removeClass(["seat-in-your-cart", "seat-sold", "seat-in-cart", "seat-available"]).addClass("seat-" + newStatus); 
	}
	
	async function reserveSeat(seatId, cart) {
		// Retrieve the concert from the URL
		const concertId = getCurrentConcert();
		let response;
		let restEndpoint;
		let payload;
		let cartId;
		if (cart) {
			// Add to the existing cart
			restEndpoint = `/concerts/${concertId}/shopping-carts/${cart}/seats`;
			payload = {seats:[seatId]};
			cartId = cart;
		}
		else {
			// Create a new shopping cart
			restEndpoint = `/concerts/${concertId}/shopping-carts`;
			cartId = Math.floor(Math.random() * 1000000) + "-" 
					+ Math.floor(Math.random() * 1000) + '-' 
					+ Math.floor(Math.random() * 100000000);
			payload = {id: cartId, seats: [seatId]};
		}
		response = await fetch(
			restEndpoint,
			{
				headers: {
					"Content-Type": "application/json"
				},
				method: 'POST',
				body: JSON.stringify(payload)
			}
		);
		if (response.ok) {
			cart = cartId;
			updateSeatStatus(seatId, 1, cart);
			return true;
		}
		else {
			console.log('Something went wrong, please try again later');
			return false;
		}
	}
	
    console.log("Running!");
})