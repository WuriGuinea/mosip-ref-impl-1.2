package io.mosip.kernel.smsserviceprovider.nimba.dto;

import lombok.Data;

@Data
public class SmsVendorRequestDto {
	private String from;
	private String to;
	private String text;
}
