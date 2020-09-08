/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2018 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package de.symeda.sormas.app.contact.edit;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import android.view.View;

import de.symeda.sormas.api.ConfigFacade;
import de.symeda.sormas.api.CountryHelper;
import de.symeda.sormas.api.Disease;
import de.symeda.sormas.api.contact.ContactCategory;
import de.symeda.sormas.api.contact.ContactClassification;
import de.symeda.sormas.api.contact.ContactDto;
import de.symeda.sormas.api.contact.ContactIdentificationSource;
import de.symeda.sormas.api.contact.ContactProximity;
import de.symeda.sormas.api.contact.ContactRelation;
import de.symeda.sormas.api.contact.QuarantineType;
import de.symeda.sormas.api.contact.TracingApp;
import de.symeda.sormas.api.utils.YesNoUnknown;
import de.symeda.sormas.api.utils.fieldvisibility.FieldVisibilityCheckers;
import de.symeda.sormas.api.utils.fieldvisibility.checkers.CountryFieldVisibilityChecker;
import de.symeda.sormas.app.BaseEditFragment;
import de.symeda.sormas.app.R;
import de.symeda.sormas.app.backend.caze.Case;
import de.symeda.sormas.app.backend.common.DatabaseHelper;
import de.symeda.sormas.app.backend.config.ConfigProvider;
import de.symeda.sormas.app.backend.contact.Contact;
import de.symeda.sormas.app.backend.contact.ContactEditAuthorization;
import de.symeda.sormas.app.caze.edit.CaseNewActivity;
import de.symeda.sormas.app.caze.read.CaseReadActivity;
import de.symeda.sormas.app.component.Item;
import de.symeda.sormas.app.component.controls.ControlPropertyField;
import de.symeda.sormas.app.component.controls.ValueChangeListener;
import de.symeda.sormas.app.component.dialog.ConfirmationDialog;
import de.symeda.sormas.app.core.FieldHelper;
import de.symeda.sormas.app.databinding.FragmentContactEditLayoutBinding;
import de.symeda.sormas.app.util.AppFieldAccessCheckers;
import de.symeda.sormas.app.util.DataUtils;
import de.symeda.sormas.app.util.DiseaseConfigurationCache;
import de.symeda.sormas.app.util.InfrastructureHelper;

public class ContactEditFragment extends BaseEditFragment<FragmentContactEditLayoutBinding, Contact, Contact> {

	private Contact record;
	private Case sourceCase;

	// Enum lists

	private List<Item> relationshipList;
	private List<Item> contactClassificationList;
	private List<Item> quarantineList;
	private List<Item> initialRegions;
	private List<Item> initialDistricts;
	private List<Item> initialCommunities;
	private List<Item> diseaseList;
	private List<Item> categoryList;
	private List<Item> contactIdentificationSources;
	private List<Item> tracingApps;

	// Instance methods

	public static ContactEditFragment newInstance(Contact activityRootData) {
		return newInstanceWithFieldCheckers(
			ContactEditFragment.class,
			null,
			activityRootData,
			FieldVisibilityCheckers.withDisease(activityRootData.getDisease())
				.add(new CountryFieldVisibilityChecker(ConfigProvider.getServerLocale())),
			AppFieldAccessCheckers
				.withCheckers(ContactEditAuthorization.isContactEditAllowed(activityRootData), FieldHelper.createSensitiveDataFieldAccessChecker()));
	}

	private void setUpControlListeners(FragmentContactEditLayoutBinding contentBinding) {
		contentBinding.createCase.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				CaseNewActivity.startActivityFromContact(getContext(), record.getUuid());
			}
		});

		contentBinding.openSourceCase.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				CaseReadActivity.startActivity(getActivity(), sourceCase.getUuid(), true);
			}
		});

		contentBinding.openResultingCase.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				CaseReadActivity.startActivity(getActivity(), record.getResultingCaseUuid(), true);
			}
		});
	}

	private void setUpFieldVisibilities(FragmentContactEditLayoutBinding contentBinding) {
		setFieldVisibilitiesAndAccesses(ContactDto.class, contentBinding.mainContent);

		if (record.getResultingCaseUuid() != null) {
			contentBinding.createCase.setVisibility(GONE);
			if (DatabaseHelper.getCaseDao().queryUuidBasic(record.getResultingCaseUuid()) == null) {
				contentBinding.openResultingCase.setVisibility(GONE);
			}
		} else {
			contentBinding.openResultingCase.setVisibility(GONE);
		}
		if (sourceCase == null) {
			contentBinding.openSourceCase.setVisibility(GONE);
		} else {
			contentBinding.contactDisease.setVisibility(GONE);
			contentBinding.contactCaseIdExternalSystem.setVisibility(GONE);
			contentBinding.contactCaseOrEventInformation.setVisibility(GONE);
		}

		if (record.getContactClassification() != ContactClassification.CONFIRMED) {
			contentBinding.createCase.setVisibility(GONE);
		}

		if (!ConfigProvider.isConfiguredServer(CountryHelper.COUNTRY_CODE_GERMANY)) {
			contentBinding.contactImmunosuppressiveTherapyBasicDisease.setVisibility(GONE);
			contentBinding.contactImmunosuppressiveTherapyBasicDiseaseDetails.setVisibility(GONE);
			contentBinding.contactCareForPeopleOver60.setVisibility(GONE);
			contentBinding.contactExternalID.setVisibility(GONE);
		} else {
			contentBinding.contactImmunosuppressiveTherapyBasicDisease.addValueChangedListener(e -> {
				if (YesNoUnknown.YES.equals(e.getValue())) {
					contentBinding.contactHighPriority.setValue(true);
				}
			});
			contentBinding.contactCareForPeopleOver60.addValueChangedListener(e -> {
				if (YesNoUnknown.YES.equals(e.getValue())) {
					contentBinding.contactHighPriority.setValue(true);
				}
			});
		}
	}

	// Overrides

	@Override
	protected String getSubHeadingTitle() {
		return getResources().getString(R.string.caption_contact_information);
	}

	@Override
	public Contact getPrimaryData() {
		return record;
	}

	@Override
	protected void prepareFragmentData() {
		record = getActivityRootData();
		if (record.getCaseUuid() != null) {
			sourceCase = DatabaseHelper.getCaseDao().queryUuidBasic(record.getCaseUuid());
		}

		relationshipList = DataUtils.getEnumItems(ContactRelation.class, true);
		contactClassificationList = DataUtils.getEnumItems(ContactClassification.class, true);
		quarantineList = DataUtils.getEnumItems(QuarantineType.class, true);
		initialRegions = InfrastructureHelper.loadRegions();
		initialDistricts = InfrastructureHelper.loadDistricts(record.getRegion());
		initialCommunities = InfrastructureHelper.loadCommunities(record.getDistrict());
		diseaseList = DataUtils.toItems(DiseaseConfigurationCache.getInstance().getAllDiseases(true, true, true));
		categoryList = DataUtils.getEnumItems(ContactCategory.class, true);
		contactIdentificationSources = DataUtils.getEnumItems(ContactIdentificationSource.class, true);
		tracingApps = DataUtils.getEnumItems(TracingApp.class, true);
	}

	@Override
	public void onLayoutBinding(FragmentContactEditLayoutBinding contentBinding) {
		setUpControlListeners(contentBinding);

		contentBinding.setData(record);
		contentBinding.setCaze(sourceCase);

		InfrastructureHelper.initializeRegionFields(
			contentBinding.contactRegion,
			initialRegions,
			record.getRegion(),
			contentBinding.contactDistrict,
			initialDistricts,
			record.getDistrict(),
			contentBinding.contactCommunity,
			initialCommunities,
			record.getCommunity());
		contentBinding.contactDisease.initializeSpinner(diseaseList, DiseaseConfigurationCache.getInstance().getDefaultDisease());
		contentBinding.contactDisease.addValueChangedListener(e -> {
			contentBinding.contactContactProximity.setVisibility(e.getValue() == null ? GONE : VISIBLE);
			contentBinding.contactContactProximity.clear();
			contentBinding.contactContactProximity
				.setItems(DataUtils.toItems(Arrays.asList(ContactProximity.getValues((Disease) e.getValue(), ConfigProvider.getServerLocale()))));
		});

		contentBinding.contactContactProximity
			.setItems(DataUtils.toItems(Arrays.asList(ContactProximity.getValues(record.getDisease(), ConfigProvider.getServerLocale()))));

		contentBinding.contactQuarantine.addValueChangedListener(e -> {
			boolean visible = QuarantineType.HOME.equals(contentBinding.contactQuarantine.getValue())
				|| QuarantineType.INSTITUTIONELL.equals(contentBinding.contactQuarantine.getValue());
			if (visible) {
				if (ConfigProvider.isConfiguredServer(CountryHelper.COUNTRY_CODE_GERMANY)) {
					contentBinding.contactQuarantineOrderedVerbally.setVisibility(VISIBLE);
					contentBinding.contactQuarantineOrderedOfficialDocument.setVisibility(VISIBLE);
				}

				contentBinding.contactQuarantineExtended.setVisibility(VISIBLE);
			} else {
				contentBinding.contactQuarantineOrderedVerbally.setVisibility(GONE);
				contentBinding.contactQuarantineOrderedOfficialDocument.setVisibility(GONE);

				contentBinding.contactQuarantineExtended.setVisibility(GONE);
			}
		});

		contentBinding.contactQuarantineExtended.setEnabled(false);

		contentBinding.contactQuarantineTo.addValueChangedListener(new ValueChangeListener() {

			private Date currentQuarantineTo = record.getQuarantineTo();
			private boolean currentQuarantineExtended = record.isQuarantineExtended();

			@Override
			public void onChange(ControlPropertyField e) {
				Date newQuarantineTo = (Date) e.getValue();

				if (currentQuarantineTo != null && newQuarantineTo != null && newQuarantineTo.compareTo(currentQuarantineTo) > 0) {
					final ConfirmationDialog confirmationDialog = new ConfirmationDialog(
						getActivity(),
						R.string.heading_extend_quarantine,
						R.string.confirmation_extend_quarantine,
						R.string.yes,
						R.string.no);

					confirmationDialog.setPositiveCallback(() -> {
						contentBinding.contactQuarantineExtended.setValue(true);
					});
					confirmationDialog.setNegativeCallback(() -> contentBinding.contactQuarantineTo.setValue(currentQuarantineTo));

					confirmationDialog.show();
				} else if (!currentQuarantineExtended) {
					contentBinding.contactQuarantineExtended.setValue(false);
				}
			}
		});

		contentBinding.contactContactIdentificationSource.addValueChangedListener(e -> {
			if (ContactIdentificationSource.TRACING_APP.equals(e.getValue())) {
				contentBinding.contactTracingApp.setVisibility(VISIBLE);
			} else {
				contentBinding.contactTracingApp.setVisibility(GONE);
				contentBinding.contactTracingApp.setValue(null);
				contentBinding.contactTracingAppDetails.setVisibility(GONE);
				contentBinding.contactTracingAppDetails.setValue("");
			}
		});
		if (ConfigProvider.isConfiguredServer(CountryHelper.COUNTRY_CODE_GERMANY)) {
			contentBinding.contactContactProximity.addValueChangedListener(
				e -> updateContactCategory(contentBinding, (ContactProximity) contentBinding.contactContactProximity.getValue()));
		} else {
			contentBinding.contactContactIdentificationSource.setVisibility(GONE);
			contentBinding.contactContactProximityDetails.setVisibility(GONE);
			contentBinding.contactContactCategory.setVisibility(GONE);
			contentBinding.contactQuarantineOrderedVerbally.setVisibility(GONE);
			contentBinding.contactQuarantineOrderedVerballyDate.setVisibility(GONE);
			contentBinding.contactQuarantineOrderedOfficialDocument.setVisibility(GONE);
			contentBinding.contactQuarantineOrderedOfficialDocumentDate.setVisibility(GONE);
		}

		if (record.getCaseUuid() != null) {
			contentBinding.contactDisease.setVisibility(GONE);
			contentBinding.contactCaseIdExternalSystem.setVisibility(GONE);
			contentBinding.contactCaseOrEventInformation.setVisibility(GONE);
		} else {
			contentBinding.contactDisease.setRequired(true);
			contentBinding.contactRegion.setRequired(true);
			contentBinding.contactDistrict.setRequired(true);
		}

		ContactValidator.initializeValidation(record, contentBinding);

		//contentBinding.setContactProximityClass(ContactProximity.class);
	}

	/*
	 * Only used for Systems in Germany. Follows specific rules for german systems.
	 */
	private void updateContactCategory(FragmentContactEditLayoutBinding contentBinding, ContactProximity proximity) {
		if (proximity != null) {
			switch (proximity) {
			case FACE_TO_FACE_LONG:
			case TOUCHED_FLUID:
			case AEROSOL:
				contentBinding.contactContactCategory.setValue(ContactCategory.HIGH_RISK);
				break;
			case MEDICAL_UNSAFE:
				contentBinding.contactContactCategory.setValue(ContactCategory.HIGH_RISK_MED);
				break;
			case MEDICAL_LIMITED:
				contentBinding.contactContactCategory.setValue(ContactCategory.MEDIUM_RISK_MED);
				break;
			case SAME_ROOM:
			case FACE_TO_FACE_SHORT:
			case MEDICAL_SAME_ROOM:
				contentBinding.contactContactCategory.setValue(ContactCategory.LOW_RISK);
				break;
			case MEDICAL_DISTANT:
			case MEDICAL_SAFE:
				contentBinding.contactContactCategory.setValue(ContactCategory.NO_RISK);
				break;
			default:
			}
		}
	}

	@Override
	public void onAfterLayoutBinding(FragmentContactEditLayoutBinding contentBinding) {
		setUpFieldVisibilities(contentBinding);

		// Initialize ControlSpinnerFields
		contentBinding.contactRelationToCase.initializeSpinner(relationshipList);
		contentBinding.contactContactClassification.initializeSpinner(contactClassificationList);
		contentBinding.contactQuarantine.initializeSpinner(quarantineList);
		contentBinding.contactContactCategory.initializeSpinner(categoryList);
		contentBinding.contactContactIdentificationSource.initializeSpinner(contactIdentificationSources);
		contentBinding.contactTracingApp.initializeSpinner(tracingApps);

		// Initialize ControlDateFields
		contentBinding.contactLastContactDate.initializeDateField(getFragmentManager());
		contentBinding.contactReportDateTime.initializeDateField(getFragmentManager());
		contentBinding.contactQuarantineFrom.initializeDateField(getFragmentManager());
		contentBinding.contactQuarantineTo.initializeDateField(getFragmentManager());
		contentBinding.contactQuarantineOrderedVerballyDate.initializeDateField(getChildFragmentManager());
		contentBinding.contactQuarantineOrderedOfficialDocumentDate.initializeDateField(getChildFragmentManager());
	}

	@Override
	public int getEditLayout() {
		return R.layout.fragment_contact_edit_layout;
	}
}
