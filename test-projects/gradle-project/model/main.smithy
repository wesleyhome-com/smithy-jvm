$version: "2"

namespace com.example.library

@documentation("The central service for managing the library, including catalog search, circulation, reservations, and patron management.")
service LibraryService {
    version: "2024-03-17"
    operations: [
        // Catalog Domain
        GetMedia
        SearchCatalog
        AddMediaItem
        // Circulation Domain
        CheckOutItem
        CheckInItem
        // Reservations Domain
        ReserveComputer
        ListReservations
        CancelReservation
        // Patron Domain
        GetPatronInfo
        UpdatePatronContact
    ]
}

// --- Catalog Operations ---
@documentation("Retrieves details of a specific media item by its ID.")
@http(method: "GET", uri: "/catalog/{id}")
@readonly
@tags(["Catalog"])
operation GetMedia {
    input: GetMediaInput
    output: GetMediaOutput
    errors: [
        NotFoundError
        InternalServerError
    ]
}

@documentation("Adds a new media item to the library's catalog.")
@http(method: "POST", uri: "/catalog")
@tags(["Catalog"])
operation AddMediaItem {
    input: AddMediaItemInput
    output: AddMediaItemOutput
    errors: [
        InvalidInputError
        InternalServerError
    ]
}

@documentation("Searches the library catalog for media items based on query parameters.")
@http(method: "GET", uri: "/catalog")
@readonly
@tags(["Catalog"])
operation SearchCatalog {
    input: SearchCatalogInput
    output: SearchCatalogOutput
}

// --- Circulation Operations ---
@documentation("Checks out a media item to a specific patron.")
@http(method: "POST", uri: "/circulation/checkout")
@tags(["Circulation"])
operation CheckOutItem {
    input: CheckOutItemInput
    output: CheckOutItemOutput
    errors: [
        NotFoundError
        ConflictError
        InternalServerError
    ]
}

@documentation("Checks in a previously checked out media item using its loan ID.")
@http(method: "POST", uri: "/circulation/checkin/{loanId}")
@tags(["Circulation"])
operation CheckInItem {
    input: CheckInItemInput
    output: CheckInItemOutput
    errors: [
        NotFoundError
        InternalServerError
    ]
}

// --- Reservations Operations ---
@documentation("Reserves a computer resource for a specific patron for a specified duration.")
@http(method: "POST", uri: "/reservations/computers")
@tags(["Reservations"])
operation ReserveComputer {
    input: ReserveComputerInput
    output: ReserveComputerOutput
    errors: [
        InvalidInputError
        ConflictError
        InternalServerError
    ]
}

@documentation("Retrieves a list of active reservations, optionally filtered by patron.")
@http(method: "GET", uri: "/reservations")
@readonly
@tags(["Reservations"])
operation ListReservations {
    input: ListReservationsInput
    output: ListReservationsOutput
}

@documentation("Cancels an existing reservation by its ID.")
@http(method: "DELETE", uri: "/reservations/{id}")
@idempotent
@tags(["Reservations"])
operation CancelReservation {
    input: CancelReservationInput
    output: CancelReservationOutput
    errors: [
        NotFoundError
        InternalServerError
    ]
}

// --- Patron Operations ---
@documentation("Retrieves the details of a patron by their ID.")
@http(method: "GET", uri: "/patrons/{id}")
@readonly
@tags(["Patron"])
operation GetPatronInfo {
    input: GetPatronInfoInput
    output: GetPatronInfoOutput
    errors: [
        NotFoundError
        InternalServerError
    ]
}

@documentation("Updates the contact information for a specific patron.")
@http(method: "PATCH", uri: "/patrons/{id}/contact")
@tags(["Patron"])
operation UpdatePatronContact {
    input: UpdatePatronContactInput
    output: UpdatePatronContactOutput
    errors: [
        NotFoundError
        InvalidInputError
        InternalServerError
    ]
}

// --- Domain Shapes ---
@documentation("The input shape for GetMedia operation.")
@tags(["Catalog"])
structure GetMediaInput {
    @documentation("The unique identifier of the media item to retrieve.")
    @required
    @httpLabel
    id: String
}

@documentation("The output shape for GetMedia operation.")
@tags(["Catalog"])
structure GetMediaOutput {
    @documentation("The media item details.")
    @required
    item: MediaItem
}

@documentation("The output shape for AddMediaItem operation.")
@tags(["Catalog"])
structure AddMediaItemOutput {
    @documentation("The media item that was added.")
    @required
    item: MediaItem
}

@documentation("The input shape for AddMediaItem operation.")
@tags(["Catalog"])
structure AddMediaItemInput {
    @documentation("The media item details to add to the catalog.")
    @required
    @httpPayload
    item: MediaItem
}

@documentation("The input shape for SearchCatalog operation.")
@tags(["Catalog"])
structure SearchCatalogInput {
    @documentation("The search query string.")
    @httpQuery("q")
    query: String

    @documentation("The type of media to filter by.")
    @httpQuery("type")
    type: MediaType

    @documentation("The page number for pagination, starting from 0.")
    @httpQuery("page")
    @range(min: 0)
    @default(0)
    page: Integer
}

@documentation("The output shape for SearchCatalog operation.")
@tags(["Catalog"])
structure SearchCatalogOutput {
    @documentation("The list of media items matching the search criteria.")
    @required
    items: MediaList

    @documentation("The total number of results found.")
    @required
    total: Long
}

@documentation("The input shape for CheckOutItem operation.")
@tags(["Circulation"])
structure CheckOutItemInput {
    @documentation("The request payload containing checkout details.")
    @required
    @httpPayload
    request: CheckOutRequest
}

@documentation("The output shape for CheckOutItem operation.")
@tags(["Circulation"])
structure CheckOutItemOutput {
    @documentation("The created loan record for the checkout.")
    @required
    loan: LoanRecord
}

@documentation("Details required to check out a media item.")
@tags(["Circulation"])
structure CheckOutRequest {
    @documentation("The unique identifier of the patron checking out the item.")
    @required
    patronId: String

    @documentation("The unique identifier of the media item to check out.")
    @required
    itemId: String
}

@documentation("The input shape for CheckInItem operation.")
@tags(["Circulation"])
structure CheckInItemInput {
    @documentation("The unique identifier of the loan to check in.")
    @required
    @httpLabel
    loanId: String
}

@documentation("The output shape for CheckInItem operation. Empty on success.")
@tags(["Circulation"])
structure CheckInItemOutput {}

@documentation("The input shape for ReserveComputer operation.")
@tags(["Reservations"])
structure ReserveComputerInput {
    @documentation("The payload containing details for the computer reservation.")
    @required
    @httpPayload
    request: ComputerReservationDetails
}

@documentation("The output shape for ReserveComputer operation.")
@tags(["Reservations"])
structure ReserveComputerOutput {
    @documentation("The created reservation details.")
    @required
    reservation: Reservation
}

@documentation("Details required to reserve a computer resource.")
@tags(["Reservations"])
structure ComputerReservationDetails {
    @documentation("The unique identifier of the patron reserving the computer.")
    @required
    patronId: String

    @documentation("The unique identifier of the computer resource to reserve.")
    @required
    computerId: String

    @documentation("The desired start time for the reservation.")
    @required
    startTime: Timestamp

    @documentation("The duration of the reservation in minutes (between 15 and 120).")
    @range(min: 15, max: 120)
    @default(60)
    durationMinutes: Integer
}

@documentation("The input shape for CancelReservation operation.")
@tags(["Reservations"])
structure CancelReservationInput {
    @documentation("The unique identifier of the reservation to cancel.")
    @required
    @httpLabel
    id: String
}

@documentation("The output shape for CancelReservation operation. Empty on success.")
@tags(["Reservations"])
structure CancelReservationOutput {}

@documentation("The input shape for ListReservations operation.")
@tags(["Reservations"])
structure ListReservationsInput {
    @documentation("Optional patron ID to filter reservations by patron.")
    @httpQuery("patronId")
    patronId: String
}

@documentation("The output shape for ListReservations operation.")
@tags(["Reservations"])
structure ListReservationsOutput {
    @documentation("The list of active reservations.")
    @required
    reservations: ReservationList
}

@documentation("The input shape for GetPatronInfo operation.")
@tags(["Patron"])
structure GetPatronInfoInput {
    @documentation("The unique identifier of the patron.")
    @required
    @httpLabel
    id: String
}

@documentation("The output shape for GetPatronInfo operation.")
@tags(["Patron"])
structure GetPatronInfoOutput {
    @documentation("The details of the requested patron.")
    @required
    patron: Patron
}

@documentation("The input shape for UpdatePatronContact operation.")
@tags(["Patron"])
structure UpdatePatronContactInput {
    @documentation("The unique identifier of the patron to update.")
    @required
    @httpLabel
    id: String

    @documentation("The new contact details to apply.")
    @required
    @httpPayload
    contact: ContactDetails
}

@documentation("The output shape for UpdatePatronContact operation.")
@tags(["Patron"])
structure UpdatePatronContactOutput {
    @documentation("The updated patron details.")
    @required
    patron: Patron
}

// --- Models ---
@documentation("A polymorphic type representing any catalog media item.")
@tags(["Catalog"])
union MediaItem {
    @documentation("Details for a book item.")
    book: BookDetails

    @documentation("Details for a magazine item.")
    magazine: MagazineDetails

    @documentation("Details for a CD item.")
    cd: CdDetails

    @documentation("Details for a movie item.")
    movie: MovieDetails
}

@documentation("Details specifically for books.")
@tags(["Catalog"])
structure BookDetails {
    @documentation("The International Standard Book Number (ISBN).")
    @required
    isbn: String

    @documentation("The title of the book.")
    @required
    title: String

    @documentation("The author of the book.")
    @required
    author: String

    @documentation("The total number of pages in the book.")
    pages: Integer
}

@documentation("Details specifically for magazines.")
@tags(["Catalog"])
structure MagazineDetails {
    @documentation("The title of the magazine.")
    @required
    title: String

    @documentation("The issue number or designation.")
    @required
    issueNumber: String

    @documentation("The date the magazine was published.")
    @required
    publishDate: Timestamp
}

@documentation("Details specifically for CDs (Compact Discs).")
@tags(["Catalog"])
structure CdDetails {
    @documentation("The title of the CD.")
    @required
    title: String

    @documentation("The main artist or band name.")
    @required
    artist: String

    @documentation("The number of tracks on the CD.")
    @required
    trackCount: Integer
}

@documentation("Details specifically for movies.")
@tags(["Catalog"])
structure MovieDetails {
    @documentation("The title of the movie.")
    @required
    title: String

    @documentation("The director of the movie.")
    @required
    director: String

    @documentation("The running time of the movie in minutes.")
    @required
    @range(min: 1)
    durationMinutes: Integer

    @documentation("The age or content rating of the movie (e.g., PG-13, R).")
    rating: String
}

@documentation("A record of a borrowed item.")
@tags(["Circulation"])
structure LoanRecord {
    @documentation("The unique identifier for the loan.")
    @required
    loanId: String

    @documentation("The date and time when the item is due to be returned.")
    @required
    dueDate: Timestamp
}

@documentation("A reservation for a specific resource, such as a computer.")
@tags(["Reservations"])
structure Reservation {
    @documentation("The unique identifier for the reservation.")
    @required
    reservationId: String

    @documentation("The identifier of the reserved resource.")
    @required
    resourceId: String

    @documentation("The current status of the reservation.")
    @required
    status: ReservationStatus
}

@documentation("Information about a library patron.")
@tags(["Patron"])
structure Patron {
    @documentation("The unique identifier for the patron.")
    @required
    id: String

    @documentation("The full name of the patron.")
    @required
    name: String

    @documentation("The contact details for the patron.")
    @required
    contact: ContactDetails

    @documentation("The current membership status of the patron.")
    @required
    membershipStatus: MembershipStatus
}

@documentation("Contact information for a user.")
@tags(["Patron"])
structure ContactDetails {
    @documentation("The primary email address.")
    @required
    @pattern("^\\S+@\\S+\\.\\S+$")
    email: String

    @documentation("The primary phone number.")
    phone: String
}

@documentation("The current status of a patron's membership.")
@tags(["Patron"])
enum MembershipStatus {
    @documentation("The patron's account is active and in good standing.")
    ACTIVE = "active"

    @documentation("The patron's account has been temporarily suspended.")
    SUSPENDED = "suspended"

    @documentation("The patron's membership has expired and needs renewal.")
    EXPIRED = "expired"
}

@documentation("The type of a media item in the catalog.")
@tags(["Catalog"])
enum MediaType {
    @documentation("A traditional printed or electronic book.")
    BOOK = "book"

    @documentation("A periodical publication.")
    MAGAZINE = "magazine"

    @documentation("An audio compact disc.")
    CD = "cd"

    @documentation("A film or video release.")
    MOVIE = "movie"
}

@documentation("The state of a resource reservation.")
@tags(["Reservations"])
enum ReservationStatus {
    @documentation("The reservation has been requested but not yet confirmed.")
    PENDING = "pending"

    @documentation("The reservation has been confirmed and is scheduled.")
    CONFIRMED = "confirmed"

    @documentation("The reservation was cancelled before it occurred.")
    CANCELLED = "cancelled"

    @documentation("The reservation time passed without it being utilized or formally cancelled.")
    EXPIRED = "expired"
}

@documentation("A list of media items.")
list MediaList {
    @documentation("A media item element within the list.")
    member: MediaItem
}

@documentation("A list of reservations.")
list ReservationList {
    @documentation("A reservation element within the list.")
    member: Reservation
}

// --- Errors ---
@documentation("Client error indicating the requested resource could not be found.")
@error("client")
@httpError(404)
structure NotFoundError {
    @documentation("A human-readable description of the error.")
    @required
    message: String
}

@documentation("Client error indicating invalid input was provided.")
@error("client")
@httpError(400)
structure InvalidInputError {
    @documentation("A human-readable description of the error.")
    @required
    message: String

    @documentation("An optional detail explaining why the input was invalid.")
    reason: String
}

@documentation("Client error indicating a conflict with the current state of the resource.")
@error("client")
@httpError(409)
structure ConflictError {
    @documentation("A human-readable description of the error.")
    @required
    message: String
}

@documentation("Server error indicating an unexpected internal failure occurred.")
@error("server")
@httpError(500)
structure InternalServerError {
    @documentation("A human-readable description of the error.")
    @required
    message: String
}
