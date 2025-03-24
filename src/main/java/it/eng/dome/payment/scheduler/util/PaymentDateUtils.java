package it.eng.dome.payment.scheduler.util;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;

public class PaymentDateUtils {

	public static final SimpleDateFormat dateformat = new SimpleDateFormat("HH:mm:ss");
	
	public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");
	
}
