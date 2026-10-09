package library_management.service;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import library_management.exception.BookingCannotBeCancelledException;
import library_management.exception.BookingNotFoundException;
import library_management.exception.InvalidBookingRequestException;
import library_management.exception.SeatAlreadyBookedException;
import library_management.exception.SeatNotFoundException;
import library_management.exception.UnauthorizedBookingAccessException;
import library_management.model.Booking;
import library_management.model.Seat;
import library_management.model.User;
import library_management.repository.BookingRepository;
import library_management.repository.SeatRepository;
import library_management.repository.UserRepository;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final MongoTemplate mongoTemplate;

    public BookingService(BookingRepository bookingRepository, SeatRepository seatRepository,
            UserRepository userRepository, UserService userService, MongoTemplate mongoTemplate) {
        this.bookingRepository = bookingRepository;
        this.seatRepository = seatRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.mongoTemplate = mongoTemplate;
    }

    public Booking createBooking(String studentId, CreateBookingRequest request) {
        userRepository.findByStudentId(studentId).ifPresent(user -> {
            if (user.isBlocked()) {
                throw new library_management.exception.UserBlockedException(
                    "Blocked from booking. AI Anomaly Detection: You have been blocked due to frequent seat cancellations (more than 3 times). Contact admin to enable booking.");
            }
        });

        LocalDateTime bookingStart = LocalDateTime.of(request.bookingDate(), request.startTime());
        if (!bookingStart.isAfter(LocalDateTime.now())) {
            throw new InvalidBookingRequestException("Booking start time must be in the future.");
        }
        if (request.bookingDate().isBefore(LocalDateTime.now().toLocalDate())) {
            throw new InvalidBookingRequestException("bookingDate must be today or in the future.");
        }
        if (!request.startTime().isBefore(request.endTime())) {
            throw new InvalidBookingRequestException("startTime must be before endTime.");
        }

        List<Seat> matchingSeats = seatRepository.findBySeatNumber(request.seatId());
        if (matchingSeats.isEmpty()) {
            throw new SeatNotFoundException(request.seatId());
        }
        if (matchingSeats.size() > 1) {
            throw new InvalidBookingRequestException("Seat number is ambiguous: " + request.seatId());
        }

        Seat requestedSeat = matchingSeats.get(0);
        Seat seat = mongoTemplate.findAndModify(
            Query.query(Criteria.where("_id").is(requestedSeat.getId())
                        .and("status").is(Seat.Status.AVAILABLE)),
                new Update().set("status", Seat.Status.BOOKED),
                FindAndModifyOptions.options().returnNew(true),
                Seat.class);

        if (seat == null) {
            throw new SeatAlreadyBookedException(request.seatId());
        }

        Booking booking = new Booking();
        booking.setStudentId(studentId);
        booking.setSeatId(seat.getSeatNumber());
        booking.setSeatDocumentId(seat.getId());
        booking.setSeatNumber(seat.getSeatNumber());
        booking.setFloor(seat.getFloor());
        booking.setBookingDate(request.bookingDate());
        booking.setStartTime(request.startTime());
        booking.setEndTime(request.endTime());
        booking.setStatus(Booking.Status.CONFIRMED);

        try {
            return bookingRepository.save(booking);
        } catch (RuntimeException exception) {
            mongoTemplate.updateFirst(
                    Query.query(Criteria.where("_id").is(seat.getId())
                            .and("status").is(Seat.Status.BOOKED)),
                    new Update().set("status", Seat.Status.AVAILABLE),
                    Seat.class);
            throw exception;
        }
    }

    public List<Booking> getMyBookings(String studentId) {
        completeExpiredBookings();
        return bookingRepository.findByStudentIdOrderByBookingDateDesc(studentId);
    }

    public List<Booking> getAllBookings() {
        completeExpiredBookings();
        return bookingRepository.findAll();
    }

    public int completeExpiredBookings() {
        LocalDateTime now = LocalDateTime.now();
        Criteria expiredBookingTime = new Criteria().orOperator(
                Criteria.where("bookingDate").lt(now.toLocalDate()),
                new Criteria().andOperator(
                        Criteria.where("bookingDate").is(now.toLocalDate()),
                        Criteria.where("endTime").lte(now.toLocalTime())));
        Query expiredBookingsQuery = Query.query(new Criteria().andOperator(
                Criteria.where("status").is(Booking.Status.CONFIRMED),
                expiredBookingTime));

        List<Booking> expiredBookings = mongoTemplate.find(expiredBookingsQuery, Booking.class);
        int completedCount = 0;
        for (Booking booking : expiredBookings) {
            Booking completed = mongoTemplate.findAndModify(
                    Query.query(Criteria.where("_id").is(booking.getId())
                            .and("status").is(Booking.Status.CONFIRMED)),
                    new Update().set("status", Booking.Status.COMPLETED),
                    FindAndModifyOptions.options().returnNew(true),
                    Booking.class);
            if (completed == null) {
                continue;
            }

            Criteria seatCriteria = booking.getSeatDocumentId() != null
                    ? Criteria.where("_id").is(booking.getSeatDocumentId())
                    : Criteria.where("seatNumber").is(booking.getSeatNumber())
                            .and("floor").is(booking.getFloor());
            mongoTemplate.updateFirst(
                    Query.query(seatCriteria.and("status").is(Seat.Status.BOOKED)),
                    new Update().set("status", Seat.Status.AVAILABLE),
                    Seat.class);
            completedCount++;
        }
        return completedCount;
    }

    public Booking getBooking(String bookingId, String requesterId, boolean admin) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));
        ensureCanAccess(booking, requesterId, admin);
        return booking;
    }

    public Booking cancelBooking(String bookingId, String requesterId, boolean admin) {
        completeExpiredBookings();
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));
        ensureCanAccess(booking, requesterId, admin);

        if (booking.getStatus() != Booking.Status.CONFIRMED) {
            throw new BookingCannotBeCancelledException(bookingId);
        }

        Query bookingQuery = Query.query(Criteria.where("_id").is(bookingId)
                .and("status").is(Booking.Status.CONFIRMED));
        Booking cancelled = mongoTemplate.findAndModify(bookingQuery,
                new Update().set("status", Booking.Status.CANCELLED),
                FindAndModifyOptions.options().returnNew(true), Booking.class);

        if (cancelled == null) {
            throw new BookingCannotBeCancelledException(bookingId);
        }

        Criteria seatCriteria = booking.getSeatDocumentId() != null
            ? Criteria.where("_id").is(booking.getSeatDocumentId())
            : Criteria.where("seatNumber").is(booking.getSeatNumber())
                .and("floor").is(booking.getFloor());
        mongoTemplate.updateFirst(
            Query.query(seatCriteria.and("status").is(Seat.Status.BOOKED)),
                new Update().set("status", Seat.Status.AVAILABLE),
                Seat.class);

        if (!admin) {
            userService.recordStudentBookingCancellation(booking.getStudentId());
        }

        return cancelled;
    }

    private void ensureCanAccess(Booking booking, String requesterId, boolean admin) {
        if (!admin && !booking.getStudentId().equals(requesterId)) {
            throw new UnauthorizedBookingAccessException();
        }
    }

    public record CreateBookingRequest(String seatId, LocalDate bookingDate,
            java.time.LocalTime startTime, java.time.LocalTime endTime) { }
}