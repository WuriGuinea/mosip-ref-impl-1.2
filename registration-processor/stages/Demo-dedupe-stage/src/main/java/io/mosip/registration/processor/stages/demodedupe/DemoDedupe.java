package io.mosip.registration.processor.stages.demodedupe;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import io.mosip.authentication.core.dto.indauth.AuthRequestDTO;
import io.mosip.authentication.core.dto.indauth.AuthResponseDTO;
import io.mosip.authentication.core.dto.indauth.AuthTypeDTO;
import io.mosip.authentication.core.dto.indauth.IdentityDTO;
import io.mosip.authentication.core.dto.indauth.IdentityInfoDTO;
import io.mosip.authentication.core.dto.indauth.RequestDTO;
import io.mosip.registration.processor.core.code.ApiName;
import io.mosip.registration.processor.core.constant.JsonConstant;
import io.mosip.registration.processor.core.exception.ApisResourceAccessException;
import io.mosip.registration.processor.core.packet.dto.Identity;
import io.mosip.registration.processor.core.packet.dto.demographicinfo.DemographicInfoDto;
import io.mosip.registration.processor.core.spi.filesystem.adapter.FileSystemAdapter;
import io.mosip.registration.processor.core.spi.packetmanager.PacketInfoManager;
import io.mosip.registration.processor.core.spi.restclient.RegistrationProcessorRestClientService;
import io.mosip.registration.processor.filesystem.ceph.adapter.impl.utils.PacketFiles;
import io.mosip.registration.processor.packet.storage.dao.PacketInfoDao;
import io.mosip.registration.processor.packet.storage.dto.ApplicantInfoDto;

/**
 * The Class DemoDedupe.
 *
 * @author M1048358 Alok Ranjan
 * @author M1048860 Kiran Raj
 */
@Component
public class DemoDedupe {

	/** The Constant FILE_SEPARATOR. */
	private static final String FILE_SEPARATOR = "\\";

	/** The Constant BIOMETRIC_APPLICANT. */
	private static final String BIOMETRIC_APPLICANT = PacketFiles.BIOMETRIC.name() + FILE_SEPARATOR
			+ PacketFiles.APPLICANT.name() + FILE_SEPARATOR;

	/** The adapter. */
	@Autowired
	private FileSystemAdapter<InputStream, Boolean> adapter;

	/** The rest client service. */
	@Autowired
	private RegistrationProcessorRestClientService<Object> restClientService;

	/** The auth request DTO. */
	private AuthRequestDTO authRequestDTO = new AuthRequestDTO();

	/** The auth type DTO. */
	private AuthTypeDTO authTypeDTO = new AuthTypeDTO();

	/** The identity DTO. */
	private IdentityDTO identityDTO = new IdentityDTO();

	/** The identity info DTO. */
	private IdentityInfoDTO identityInfoDTO = new IdentityInfoDTO();

	/** The request. */
	private RequestDTO request = new RequestDTO();

	/** The packet info manager. */
	@Autowired
	private PacketInfoManager<Identity, ApplicantInfoDto> packetInfoManager;

	/** The packet info dao. */
	@Autowired
	private PacketInfoDao packetInfoDao;

	/**
	 * Perform demodedupe.
	 *
	 * @param refId the ref id
	 * @return duplicate Ids
	 */
	public List<DemographicInfoDto> performDedupe(String refId) {

		List<DemographicInfoDto> applicantDemoDto = packetInfoDao.findDemoById(refId);
		List<DemographicInfoDto> demographicInfoDtos = new ArrayList<>();
		for (DemographicInfoDto demoDto : applicantDemoDto) {

			demographicInfoDtos.addAll(packetInfoDao.getAllDemographicInfoDtos(demoDto.getPhoneticName(),
					demoDto.getGenderCode(), demoDto.getDob(), demoDto.getLangCode()));
		}

		return demographicInfoDtos;
	}

	/**
	 * Authenticate duplicates.
	 *
	 * @param regId
	 *            the reg id
	 * @param duplicateUins
	 *            the duplicate ids
	 * @return true, if successful
	 * @throws ApisResourceAccessException
	 *             the apis resource access exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public boolean authenticateDuplicates(String regId, List<String> duplicateUins)
			throws ApisResourceAccessException, IOException {

		List<String> applicantfingerprintImageNames = packetInfoManager.getApplicantFingerPrintImageNameById(regId);
		List<String> applicantIrisImageNames = packetInfoManager.getApplicantIrisImageNameById(regId);
		boolean isDuplicate = false;

		for (String duplicateUin : duplicateUins) {
			setAuthDto();

			if (authenticateFingerBiometric(applicantfingerprintImageNames, PacketFiles.FINGER.name(), duplicateUin, regId)
					|| authenticateIrisBiometric(applicantIrisImageNames, PacketFiles.IRIS.name(), duplicateUin, regId)) {
				isDuplicate = true;
				break;
			}
		}

		return isDuplicate;

	}

	/**
	 * Authenticate biometric.
	 *
	 * @param biometriclist
	 *            the biometriclist
	 * @param type
	 *            the type
	 * @param duplicateUin
	 *            the duplicate id
	 * @param regId
	 *            the reg id
	 * @return true, if successful
	 * @throws ApisResourceAccessException
	 *             the apis resource access exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private boolean authenticateFingerBiometric(List<String> biometriclist, String type, String duplicateUin, String regId)
			throws ApisResourceAccessException, IOException {

		for (String biometricName : biometriclist) {
			String biometric = BIOMETRIC_APPLICANT + biometricName.toUpperCase();

			if (adapter.checkFileExistence(regId, biometric)) {
				InputStream biometricFileName = adapter.getFile(regId, biometric);
				byte[] fingerPrintByte = IOUtils.toByteArray(biometricFileName);

				setAuthDto();
				identityInfoDTO.setValue(new String(fingerPrintByte));
				List<IdentityInfoDTO> biometricData = new ArrayList<>();
				biometricData.add(identityInfoDTO);

				//authTypeDTO.setFingerPrint(true);
				switch (biometricName.toUpperCase()) {
				case JsonConstant.LEFTTHUMB:
					identityDTO.setLeftThumb(biometricData);
					break;
				case JsonConstant.LEFTINDEX:
					identityDTO.setLeftIndex(biometricData);
					break;
				case JsonConstant.LEFTMIDDLE:
					identityDTO.setLeftMiddle(biometricData);
					break;
				case JsonConstant.LEFTLITTLE:
					identityDTO.setLeftLittle(biometricData);
					break;
				case JsonConstant.LEFTRING:
					identityDTO.setLeftRing(biometricData);
					break;
				case JsonConstant.RIGHTTHUMB:
					identityDTO.setRightThumb(biometricData);
					break;
				case JsonConstant.RIGHTINDEX:
					identityDTO.setRightIndex(biometricData);
					break;
				case JsonConstant.RIGHTMIDDLE:
					identityDTO.setRightMiddle(biometricData);
					break;
				case JsonConstant.RIGHTLITTLE:
					identityDTO.setRightLittle(biometricData);
					break;
				case JsonConstant.RIGHTRING:
					identityDTO.setRightRing(biometricData);
					break;
				default:
					break;
				}

			}
		}

		return validateBiometric(duplicateUin);



	}

	/**
	 * Authenticate iris biometric.
	 *
	 * @param biometriclist the biometriclist
	 * @param type the type
	 * @param duplicateUin the duplicate uin
	 * @param regId the reg id
	 * @return true, if successful
	 * @throws ApisResourceAccessException the apis resource access exception
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private boolean authenticateIrisBiometric(List<String> biometriclist, String type, String duplicateUin, String regId)
			throws ApisResourceAccessException, IOException {
		// authTypeDTO.setIris(true);

		for (String biometricName : biometriclist) {
			String biometric = BIOMETRIC_APPLICANT + biometricName.toUpperCase();

			if (adapter.checkFileExistence(regId, biometric)) {
				InputStream biometricFileName = adapter.getFile(regId, biometric);
				byte[] biometricByte = IOUtils.toByteArray(biometricFileName);

				setAuthDto();
				identityInfoDTO.setValue(new String(biometricByte));
				List<IdentityInfoDTO> biometricData = new ArrayList<>();
				biometricData.add(identityInfoDTO);



				if (PacketFiles.LEFTEYE.name().equalsIgnoreCase(biometricName.toUpperCase())) {
					identityDTO.setLeftEye(biometricData);
				} 
				if (PacketFiles.RIGHTEYE.name().equalsIgnoreCase(biometricName.toUpperCase())) {
					identityDTO.setRightEye(biometricData);
				}
			}
		}



		return validateBiometric( duplicateUin);
	}

	/**
	 * Validate biometric.
	 *
	 * @param duplicateUin the duplicate uin
	 * @return true, if successful
	 * @throws ApisResourceAccessException             the apis resource access exception
	 */
	private boolean validateBiometric(String duplicateUin)
			throws ApisResourceAccessException {


		authRequestDTO.setIdvId(duplicateUin);

		authRequestDTO.setAuthType(authTypeDTO);
		request.setIdentity(identityDTO);
		authRequestDTO.setRequest(request);

		// sending request to get authentication response
		AuthResponseDTO authResponseDTO = (AuthResponseDTO) restClientService.postApi(ApiName.AUTHINTERNAL, "", "",
				authRequestDTO, AuthResponseDTO.class);
		return authResponseDTO != null && authResponseDTO.getStatus() != null
				&& authResponseDTO.getStatus().equalsIgnoreCase("y");
	}

	/**
	 * Sets the auth dto.
	 */
	public void setAuthDto() {
		String pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
		String date = simpleDateFormat.format(new Date());
		authRequestDTO.setReqTime(date);
		authRequestDTO.setId("mosip.internal.auth");
		authRequestDTO.setIdvIdType("D");
		//authRequestDTO.setVer("1.0");
		authTypeDTO.setAddress(false);
		authTypeDTO.setBio(false);
		authTypeDTO.setFullAddress(false);
		authTypeDTO.setOtp(false);
		authTypeDTO.setPersonalIdentity(false);
		authTypeDTO.setPin(false);
		//authTypeDTO.setFace(false);
		//authTypeDTO.setFingerPrint(false);
		//authTypeDTO.setIris(false);

	}

}
