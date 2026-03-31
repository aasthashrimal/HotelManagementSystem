package com.hotel.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import com.hotel.model.Booking;
import com.hotel.model.Customer;
import com.hotel.model.Room;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

public class PdfInvoiceGenerator {

    private static final DeviceRgb C_PRIMARY = new DeviceRgb(47, 95, 179);
    private static final DeviceRgb C_PRIMARY_DARK = new DeviceRgb(31, 63, 120);
    private static final DeviceRgb C_TINT = new DeviceRgb(241, 246, 255);
    private static final DeviceRgb C_PANEL = new DeviceRgb(249, 252, 255);
    private static final DeviceRgb C_BORDER = new DeviceRgb(188, 202, 224);
    private static final DeviceRgb C_TEXT = new DeviceRgb(31, 41, 55);
    private static final DeviceRgb C_MUTED = new DeviceRgb(100, 116, 139);
    private static final DeviceRgb C_WHITE = new DeviceRgb(255, 255, 255);

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    public static String generatePdf(Booking booking, Customer customer, Room room,
                     List<Map<String, Object>> services) {

    new File("invoices/").mkdirs();
    String path = "invoices/invoice_" + booking.getBookingId() + ".pdf";

    try (PdfWriter writer = new PdfWriter(path);
         PdfDocument pdf = new PdfDocument(writer);
         Document doc = new Document(pdf)) {
        doc.setMargins(34, 38, 30, 38);

        PdfFont headerFont = loadHeaderFont();

        renderHeader(doc, booking, headerFont);
        renderAccentRule(doc);
        renderGuestAndStayBand(doc, booking, customer, room, 
            Math.max(1, booking.getNumberOfNights()));

        // If booking is cancelled, show cancellation details; otherwise show charges
        if ("CANCELLED".equals(booking.getStatus())) {
            int nights = Math.max(1, booking.getNumberOfNights());
            double roomCharge = room.calculateTariff(nights);
            renderCancellationDetails(doc, roomCharge);
        } else {
            int nights = Math.max(1, booking.getNumberOfNights());
            double roomCharge = room.calculateTariff(nights);
            double serviceCharge = services.stream().mapToDouble(s -> toAmount(s.get("amount"))).sum();
            double subtotal = roomCharge + serviceCharge;
            double gst = subtotal * 0.12;
            double grandTotal = subtotal + gst;
            renderChargesTable(doc, room, nights, roomCharge, services, gst, grandTotal);
            renderSettlement(doc, subtotal, gst, grandTotal);
        }

        renderAuthorisation(doc);
        renderFooter(doc);

        return path;

    } catch (IOException e) {
        FileLogger.logError("PDF generation failed for booking " + booking.getBookingId() + ": " + e.getMessage());
        return null;
    }
    }

    private static void renderHeader(Document doc, Booking booking, PdfFont headerFont) {
    Table header = new Table(UnitValue.createPercentArray(new float[]{18, 52, 30}))
        .setWidth(UnitValue.createPercentValue(100))
        .setMarginBottom(4);

    Cell logoCell = new Cell().setBorder(Border.NO_BORDER).setPadding(6).setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE);
    Image logo = loadHeaderLogo();
    if (logo != null) {
        logo.scaleToFit(56, 56);
        logo.setHorizontalAlignment(HorizontalAlignment.CENTER);
        logoCell.add(logo);
    } else {
        logoCell.add(new Paragraph("A")
            .setFontSize(36)
            .setBold()
            .setFontColor(C_PRIMARY)
            .setTextAlignment(TextAlignment.CENTER));
    }

    Cell name = new Cell().setBorder(Border.NO_BORDER).setPadding(6);
    Paragraph hotel = new Paragraph("Aurelia Suites")
        .setFontSize(27)
        .setFontColor(C_PRIMARY_DARK)
        .setTextAlignment(TextAlignment.CENTER);
    if (headerFont != null) {
        hotel.setFont(headerFont);
    } else {
        hotel.setBold();
    }
    name.add(hotel);
    name.add(new Paragraph("Luxury Stays | Premium Hospitality")
        .setTextAlignment(TextAlignment.CENTER)
        .setFontSize(8.5f)
        .setFontColor(C_MUTED));
    name.add(new Paragraph("+91 90000 00000 | reservations@aureliasuites.com")
        .setTextAlignment(TextAlignment.CENTER)
        .setFontSize(7.8f)
        .setFontColor(C_MUTED));

    Cell meta = new Cell().setBorder(Border.NO_BORDER).setPadding(6)
        .setVerticalAlignment(com.itextpdf.layout.properties.VerticalAlignment.MIDDLE);
    meta.add(new Paragraph("TAX INVOICE")
        .setBold()
        .setFontSize(9)
        .setFontColor(C_PRIMARY)
        .setTextAlignment(TextAlignment.RIGHT));
    meta.add(new Paragraph("No: INV-" + booking.getBookingId())
        .setFontSize(8)
        .setFontColor(C_MUTED)
        .setTextAlignment(TextAlignment.RIGHT));
    meta.add(new Paragraph("Date: " + LocalDate.now().format(FMT))
        .setFontSize(8)
        .setFontColor(C_MUTED)
        .setTextAlignment(TextAlignment.RIGHT));

    header.addCell(logoCell);
    header.addCell(name);
    header.addCell(meta);
    doc.add(header);
    }

    private static void renderAccentRule(Document doc) {
    Table rule = new Table(UnitValue.createPercentArray(new float[]{1}))
        .setWidth(UnitValue.createPercentValue(100))
        .setMarginBottom(10);
    rule.addCell(new Cell().setBorder(Border.NO_BORDER)
        .setBackgroundColor(C_PRIMARY)
        .setHeight(1.5f)
        .setPadding(0));
    doc.add(rule);
    }

    private static void renderGuestAndStayBand(Document doc, Booking booking, Customer customer, Room room, int nights) {
    Table band = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
        .setWidth(UnitValue.createPercentValue(100))
        .setMarginBottom(12);

    Cell billedTo = panelCell("BILLED TO");
    addInfoPair(billedTo, "Guest Name", nvl(customer != null ? customer.getName() : booking.getCustomerName()));
    addInfoPair(billedTo, "Mobile", nvl(customer != null ? customer.getPhone() : null));
    addInfoPair(billedTo, "Email", nvl(customer != null ? customer.getEmail() : null));
    addInfoPair(billedTo, "ID Proof", nvl(customer != null ? customer.getIdProof() : null));

    Cell stay = panelCell("STAY DETAILS");
    addInfoPair(stay, "Room Number", "#" + booking.getRoomNumber());
    addInfoPair(stay, "Room Category", String.valueOf(room.getRoomType()));
    addInfoPair(stay, "Check-In", fmtDate(booking.getCheckIn()));
    addInfoPair(stay, "Check-Out", fmtDate(booking.getCheckOut()));
    addInfoPair(stay, "Nights", String.valueOf(nights));
    addInfoPair(stay, "Status", nvl(booking.getStatus()));

    band.addCell(billedTo);
    band.addCell(stay);
    doc.add(band);
    }

    private static void renderChargesTable(Document doc, Room room, int nights,
                       double roomCharge, List<Map<String, Object>> services,
                       double gst, double grandTotal) {
    double serviceTotal = services.stream().mapToDouble(s -> toAmount(s.get("amount"))).sum();

    Table t = new Table(UnitValue.createPercentArray(new float[]{50, 15, 20, 15}))
        .setWidth(UnitValue.createPercentValue(100))
        .setMarginBottom(10);

    addColHeader(t, "DESCRIPTION", TextAlignment.LEFT);
    addColHeader(t, "QTY", TextAlignment.RIGHT);
    addColHeader(t, "RATE (Rs.)", TextAlignment.RIGHT);
    addColHeader(t, "AMOUNT (Rs.)", TextAlignment.RIGHT);

    addItemRow(t,
        "Accommodation Charges (" + room.getRoomType() + ")",
        String.valueOf(nights),
        money(room.getPricePerNight()),
        money(roomCharge),
        false);

    for (Map<String, Object> svc : services) {
        String serviceName = nvl((String) svc.get("service_type"));
        String serviceDesc = nvl((String) svc.get("description"));
        String label = serviceDesc.equals("-") ? serviceName : serviceName + " - " + serviceDesc;
        double amt = toAmount(svc.get("amount"));
        addItemRow(t, label, "1", money(amt), money(amt), true);
    }

    addItemRow(t, "Service & Amenities Subtotal", "-", "-", money(serviceTotal), false);
    addItemRow(t, "GST / Taxes (12%)", "-", "-", money(gst), false);

    Cell totalLbl = new Cell(1, 3)
        .add(new Paragraph("GRAND TOTAL")
            .setBold()
            .setFontColor(C_PRIMARY_DARK)
            .setFontSize(11))
        .setBackgroundColor(C_TINT)
        .setBorderTop(new SolidBorder(C_BORDER, 0.8f))
        .setBorderBottom(Border.NO_BORDER)
        .setBorderLeft(Border.NO_BORDER)
        .setBorderRight(Border.NO_BORDER)
        .setTextAlignment(TextAlignment.RIGHT)
        .setPadding(8);

    Cell totalAmt = new Cell()
        .add(new Paragraph("Rs. " + money(grandTotal))
            .setBold()
            .setFontColor(C_PRIMARY)
            .setFontSize(12))
        .setBackgroundColor(C_TINT)
        .setBorderTop(new SolidBorder(C_BORDER, 0.8f))
        .setBorderBottom(Border.NO_BORDER)
        .setBorderLeft(Border.NO_BORDER)
        .setBorderRight(Border.NO_BORDER)
        .setTextAlignment(TextAlignment.RIGHT)
        .setPadding(8);

    t.addCell(totalLbl);
    t.addCell(totalAmt);

    doc.add(t);
    }

    private static void renderCancellationDetails(Document doc, double originalAmount) {
        // Calculate refund based on cancellation policy
        // Policy: 20% cancellation fee if cancelled, otherwise full refund
        double cancellationFee = originalAmount * 0.20;
        double refundAmount = originalAmount - cancellationFee;

        Table t = new Table(UnitValue.createPercentArray(new float[]{50, 15, 20, 15}))
            .setWidth(UnitValue.createPercentValue(100))
            .setMarginBottom(10);

        addColHeader(t, "DETAILS", TextAlignment.LEFT);
        addColHeader(t, "", TextAlignment.RIGHT);
        addColHeader(t, "", TextAlignment.RIGHT);
        addColHeader(t, "AMOUNT (Rs.)", TextAlignment.RIGHT);

        addItemRow(t, "Original Booking Amount", "-", "-", money(originalAmount), false);
        addItemRow(t, "Cancellation Fee (20%)", "-", "-", "- " + money(cancellationFee), true);

        Cell refundLbl = new Cell(1, 3)
            .add(new Paragraph("NET REFUND AMOUNT")
                .setBold()
                .setFontColor(C_PRIMARY_DARK)
                .setFontSize(11))
            .setBackgroundColor(C_TINT)
            .setBorderTop(new SolidBorder(C_BORDER, 0.8f))
            .setBorderBottom(Border.NO_BORDER)
            .setBorderLeft(Border.NO_BORDER)
            .setBorderRight(Border.NO_BORDER)
            .setTextAlignment(TextAlignment.RIGHT)
            .setPadding(8);

        Cell refundAmt = new Cell()
            .add(new Paragraph("Rs. " + money(refundAmount))
                .setBold()
                .setFontColor(C_PRIMARY)
                .setFontSize(12))
            .setBackgroundColor(C_TINT)
            .setBorderTop(new SolidBorder(C_BORDER, 0.8f))
            .setBorderBottom(Border.NO_BORDER)
            .setBorderLeft(Border.NO_BORDER)
            .setBorderRight(Border.NO_BORDER)
            .setTextAlignment(TextAlignment.RIGHT)
            .setPadding(8);

        t.addCell(refundLbl);
        t.addCell(refundAmt);

        doc.add(t);

        // Add cancellation policy note
        doc.add(new Paragraph(
            "Note: As per our cancellation policy, a 20% cancellation fee has been deducted from your original booking amount. " +
            "The net refund will be processed within 5-7 business days to your original payment method.")
            .setFontSize(8)
            .setFontColor(C_MUTED)
            .setMarginTop(8)
            .setMarginBottom(8));
    }

    private static void renderSettlement(Document doc, double subtotal, double gst, double total) {
    Table s = new Table(UnitValue.createPercentArray(new float[]{65, 35}))
        .setWidth(UnitValue.createPercentValue(45))
        .setHorizontalAlignment(HorizontalAlignment.RIGHT)
        .setMarginBottom(14);

    addSettlementRow(s, "Subtotal", subtotal, false);
    addSettlementRow(s, "GST (12%)", gst, false);
    addSettlementRow(s, "Balance Due", total, true);

    doc.add(s);
    }

    private static void renderAuthorisation(Document doc) {
    Table auth = new Table(UnitValue.createPercentArray(new float[]{60, 40}))
        .setWidth(UnitValue.createPercentValue(100))
        .setMarginBottom(10);

    Cell note = new Cell().setBorder(Border.NO_BORDER).setPadding(4);
    note.add(new Paragraph("This is a computer-generated invoice and does not require a physical stamp.")
        .setFontSize(8)
        .setFontColor(C_MUTED));
    note.add(new Paragraph("Please retain this invoice for your records.")
        .setFontSize(8)
        .setFontColor(C_MUTED));

    Cell signature = new Cell().setBorder(Border.NO_BORDER).setPadding(4);
    addSignatureImage(signature);
    signature.add(new LineSeparator(new SolidLine(0.8f)).setMarginTop(4).setMarginBottom(4));
    signature.add(new Paragraph("Front Office Manager")
        .setBold()
        .setFontSize(9)
        .setFontColor(C_TEXT)
        .setTextAlignment(TextAlignment.CENTER));
    signature.add(new Paragraph("Aurelia Suites")
        .setFontSize(8)
        .setFontColor(C_MUTED)
        .setTextAlignment(TextAlignment.CENTER));

    auth.addCell(note);
    auth.addCell(signature);
    doc.add(auth);
    }

    private static void renderFooter(Document doc) {
    Table rule = new Table(UnitValue.createPercentArray(new float[]{1}))
        .setWidth(UnitValue.createPercentValue(100))
        .setMarginBottom(4);
    rule.addCell(new Cell().setBorder(Border.NO_BORDER)
        .setBackgroundColor(C_PRIMARY)
        .setHeight(1f)
        .setPadding(0));
    doc.add(rule);

    doc.add(new Paragraph("Aurelia Suites | reservations@aureliasuites.com | www.aureliasuites.com")
        .setTextAlignment(TextAlignment.CENTER)
        .setFontSize(7.5f)
        .setFontColor(C_MUTED));
    }

    private static Cell panelCell(String title) {
    Cell cell = new Cell()
        .setBorder(new SolidBorder(C_BORDER, 0.8f))
        .setBackgroundColor(C_PANEL)
        .setPadding(10)
        .setMarginRight(4);

    cell.add(new Paragraph(title)
        .setBold()
        .setFontSize(8)
        .setFontColor(C_PRIMARY)
        .setMarginBottom(4));

    LineSeparator ls = new LineSeparator(new SolidLine(0.8f));
    ls.setStrokeColor(C_PRIMARY);
    ls.setWidth(UnitValue.createPercentValue(28));
    ls.setMarginBottom(6);
    cell.add(ls);
    return cell;
    }

    private static void addInfoPair(Cell cell, String label, String value) {
    Table row = new Table(UnitValue.createPercentArray(new float[]{40, 60}))
        .setWidth(UnitValue.createPercentValue(100));

    row.addCell(new Cell().add(new Paragraph(label)
            .setFontSize(8)
            .setFontColor(C_MUTED))
        .setBorder(Border.NO_BORDER)
        .setPadding(1));

    row.addCell(new Cell().add(new Paragraph(value)
            .setFontSize(8.8f)
            .setFontColor(C_TEXT))
        .setBorder(Border.NO_BORDER)
        .setPadding(1));

    cell.add(row);
    }

    private static void addColHeader(Table table, String text, TextAlignment align) {
    table.addCell(new Cell()
        .add(new Paragraph(text)
            .setBold()
            .setFontSize(8.5f)
            .setFontColor(C_PRIMARY_DARK))
        .setBackgroundColor(C_TINT)
        .setBorderBottom(new SolidBorder(C_BORDER, 0.8f))
        .setBorderTop(Border.NO_BORDER)
        .setBorderLeft(Border.NO_BORDER)
        .setBorderRight(Border.NO_BORDER)
        .setTextAlignment(align)
        .setPadding(6));
    }

    private static void addItemRow(Table table, String desc, String qty, String rate, String amount, boolean shade) {
    DeviceRgb bg = shade ? new DeviceRgb(252, 253, 255) : C_WHITE;

    table.addCell(itemCell(desc, TextAlignment.LEFT, bg));
    table.addCell(itemCell(qty, TextAlignment.RIGHT, bg));
    table.addCell(itemCell(rate, TextAlignment.RIGHT, bg));
    table.addCell(itemCell(amount, TextAlignment.RIGHT, bg));
    }

    private static Cell itemCell(String value, TextAlignment align, DeviceRgb bg) {
    return new Cell()
        .add(new Paragraph(value)
            .setFontSize(9)
            .setFontColor(C_TEXT))
        .setBackgroundColor(bg)
        .setBorderBottom(new SolidBorder(C_BORDER, 0.5f))
        .setBorderTop(Border.NO_BORDER)
        .setBorderLeft(Border.NO_BORDER)
        .setBorderRight(Border.NO_BORDER)
        .setTextAlignment(align)
        .setPadding(6);
    }

    private static void addSettlementRow(Table table, String label, double amount, boolean highlight) {
    DeviceRgb bg = highlight ? C_TINT : C_WHITE;

    Paragraph labelP = new Paragraph(label)
        .setFontSize(highlight ? 10f : 9f)
        .setFontColor(highlight ? C_PRIMARY_DARK : C_TEXT);
    if (highlight) {
        labelP.setBold();
    }

    table.addCell(new Cell()
        .add(labelP)
        .setBackgroundColor(bg)
        .setBorder(Border.NO_BORDER)
        .setTextAlignment(TextAlignment.RIGHT)
        .setPadding(5));

    Paragraph amountP = new Paragraph("Rs. " + money(amount))
        .setFontSize(highlight ? 10.5f : 9f)
        .setFontColor(highlight ? C_PRIMARY : C_TEXT);
    if (highlight) {
        amountP.setBold();
    }

    table.addCell(new Cell()
        .add(amountP)
        .setBackgroundColor(bg)
        .setBorder(Border.NO_BORDER)
        .setTextAlignment(TextAlignment.RIGHT)
        .setPadding(5));
    }

    private static void addSignatureImage(Cell signatureCell) {
    File sign = new File("sign.png");
    if (!sign.exists()) {
        signatureCell.add(new Paragraph(" ").setHeight(40));
        return;
    }

    try {
        Image signature = new Image(ImageDataFactory.create("sign.png"));
        signature.scaleToFit(130, 55);
        signature.setHorizontalAlignment(HorizontalAlignment.CENTER);
        signatureCell.add(signature);
    } catch (IOException e) {
        signatureCell.add(new Paragraph(" ").setHeight(40));
    }
    }

    private static PdfFont loadHeaderFont() {
    try {
        return PdfFontFactory.createFont(StandardFonts.TIMES_BOLD);
    } catch (IOException e) {
        return null;
    }
    }

    private static Image loadHeaderLogo() {
    String[] candidates = {
        "logo.png",
        "src/main/resources/assets/logo.png"
    };

    for (String path : candidates) {
        File file = new File(path);
        if (file.exists()) {
            try {
                return new Image(ImageDataFactory.create(path));
            } catch (IOException ignored) {
                // Try next location
            }
        }
    }

    try (InputStream in = PdfInvoiceGenerator.class.getResourceAsStream("/assets/logo.png")) {
        if (in != null) {
            return new Image(ImageDataFactory.create(in.readAllBytes()));
        }
    } catch (IOException ignored) {
        // fall through
    }
    return null;
    }

    private static double toAmount(Object value) {
    if (value instanceof Number n) {
        return n.doubleValue();
    }
    return 0.0;
    }

    private static String money(double amount) {
    return String.format("%,.2f", amount);
    }

    private static String nvl(String s) {
    return (s == null || s.isBlank()) ? "-" : s;
    }

    private static String fmtDate(LocalDate date) {
    return date == null ? "-" : date.format(FMT);
    }
}