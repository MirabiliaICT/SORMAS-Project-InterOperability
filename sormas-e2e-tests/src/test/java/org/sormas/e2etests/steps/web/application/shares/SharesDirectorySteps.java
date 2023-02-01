/*
 * SORMAS® - Surveillance Outbreak Response Management & Analysis System
 * Copyright © 2016-2022 Helmholtz-Zentrum für Infektionsforschung GmbH (HZI)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package org.sormas.e2etests.steps.web.application.shares;

import static org.sormas.e2etests.steps.web.application.shares.EditSharesPage.SHARE_FIRST_EYE_ICON;
import static org.sormas.e2etests.steps.web.application.shares.EditSharesPage.SHARE_UUID_CASE_TITLE;

import cucumber.api.java8.En;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.sormas.e2etests.helpers.WebDriverHelpers;
import org.sormas.e2etests.state.ApiState;
import org.testng.asserts.SoftAssert;

@Slf4j
public class SharesDirectorySteps implements En {

  private final WebDriverHelpers webDriverHelpers;
  public static List<String> casesUUID = new ArrayList<>();

  @Inject
  public SharesDirectorySteps(
      WebDriverHelpers webDriverHelpers, SoftAssert softly, ApiState apiState) {
    this.webDriverHelpers = webDriverHelpers;

    When(
        "I click on the The Eye Icon located in the Shares Page",
        () -> {
          webDriverHelpers.clickOnWebElementBySelector(SHARE_FIRST_EYE_ICON);
        });

    And(
        "^I check that first shared result has different id then deleted shared case$",
        () -> {
          softly.assertNotEquals(
              webDriverHelpers.getTextFromWebElement(SHARE_UUID_CASE_TITLE),
              apiState.getCreatedCase().getUuid().substring(0, 6).toUpperCase(),
              "UUIDs are equal!");
          softly.assertAll();
        });
  }
}
