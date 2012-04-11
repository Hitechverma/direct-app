/*
 * Copyright (C) 2010 - 2012 TopCoder Inc., All Rights Reserved.
 */
package com.topcoder.direct.services.view.action.contest;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.topcoder.direct.services.view.action.contest.launch.StudioOrSoftwareContestAction;
import com.topcoder.direct.services.view.dto.UserProjectsDTO;
import com.topcoder.direct.services.view.dto.contest.ContestFinalFixDTO;
import com.topcoder.direct.services.view.dto.contest.ContestRoundType;
import com.topcoder.direct.services.view.dto.contest.ContestStatsDTO;
import com.topcoder.direct.services.view.dto.contest.SoftwareContestSubmissionsDTO;
import com.topcoder.direct.services.view.dto.contest.SoftwareSubmissionDTO;
import com.topcoder.direct.services.view.dto.contest.TypedContestBriefDTO;
import com.topcoder.direct.services.view.dto.project.ProjectBriefDTO;
import com.topcoder.direct.services.view.form.ProjectIdForm;
import com.topcoder.direct.services.view.util.DataProvider;
import com.topcoder.direct.services.view.util.DirectUtils;
import com.topcoder.direct.services.view.util.SessionData;
import com.topcoder.management.resource.Resource;
import com.topcoder.project.phases.PhaseType;
import com.topcoder.security.TCSubject;
import com.topcoder.service.project.SoftwareCompetition;
import org.apache.log4j.Logger;

/**
 * <p>A <code>Struts</code> action to be used for handling requests for viewing a list of submissions for
 * <code>Software</code> contest.</p>
 * <p/>
 * <p>
 * Version 1.0.1 (Direct Release 6 Assembly 1.0) Change notes:
 * <ol>
 * <li>Updated {@link #executeAction()} method to use appropriate method for calculating contest stats.</li>
 * </ol>
 * </p>
 * <p/>
 * <p>
 * Version 1.0.2 (Direct Manage Copilot Postings Assembly 1.0) Change notes:
 * <ol>
 * <li>Updated {@link #executeAction()} method to user appropriate method for calculating contest stats.</li>
 * </ol>
 * </p>
 * <p/>
 * <p>
 * Version 1.0.3 (TC Direct Contest Dashboard Update Assembly 1.0) Change notes:
 * <ol>
 * <li>Updated {@link #executeAction()} method to set contest dashboard data.</li>
 * </ol>
 * </p>
 * <p>
  * Version 1.1 (Release Assembly - TC Direct Cockpit Release Two) Change notes:
  * <ol>
  * <li>Updated {@link #executeAction()} method to set milestone submissions data.</li>
  * </ol>
  * </p>
 *
 * <p>
 * Version 1.2 (Module Assembly - Adding Contest Approval Feature in Direct Assembly 1.0) Change notes:
 *   <ol>
 *     <li>Added logic for checking if user can perform Approval and if approval is already completed by user.</li>
 *   </ol>
 * </p>
 *
 *
 * @author TCSASSEMBLER
 * @version 1.2 (Release Assembly - TC Direct Cockpit Release Two)
 */
public class SoftwareContestSubmissionsAction extends StudioOrSoftwareContestAction {

    /**
     * <p>A <code>long</code> providing the ID for <code>Approver</code> resource role.</p>
     */
    private static final long RESOURCE_ROLE_APPROVER_ID = 10;

    /**
     * <p>A <code>long</code> providing the ID for <code>Approval</code> scorecard type.</p>
     */
    private static final long SCORECARD_TYPE_APPROVAL_ID = 3;

    /**
     * <p>Logger for this class.</p>
     */
    private static final Logger logger = Logger.getLogger(SoftwareContestSubmissionsAction.class);

    /**
     * <p>A <code>ProjectIdForm</code> providing the parameters of the incoming request.</p>
     */
    private ProjectIdForm formData;

    /**
     * <p>A <code>SessionData</code> providing interface to current session.</p>
     */
    private SessionData sessionData;

    /**
     * <p>
     *   Represents the round type of viewing software contest submission, will be set in request.
     * </p>
     * @since 1.1
     */
    private ContestRoundType roundType;

    /**
     * <p>
     *   Flag used to determine whether redirect to milestone round. It will be set to true if milestone review is
     *   open and final review is not started.
     * </p>
     * @since 1.1
     */
    private boolean redirectToMilestone;

    /**
     * <p>A <code>SoftwareContestSubmissionsDTO</code> providing the view data for displaying by <code>Software Contest
     * Submissions</code> view.</p>
     */
    private SoftwareContestSubmissionsDTO viewData;

    private List<ContestFinalFixDTO> finalFixes;

    public List<ContestFinalFixDTO> getFinalFixes() {
        return finalFixes;
    }

    public void setFinalFixes(List<ContestFinalFixDTO> finalFixes) {
        this.finalFixes = finalFixes;
    }

    /**
     * Gets the contest round type.
     *
     * @return the contest round type.
     * @since 1.1
     */
    public ContestRoundType getRoundType() {
        return roundType;
    }

    /**
     * Sets the contest round type.
     *
     * @param roundType the contest group type.
     * @since 1.1
     */
    public void setRoundType(ContestRoundType roundType) {
        this.roundType = roundType;
    }

    /**
     * <p>Constructs new <code>SoftwareContestSubmissionsAction</code> instance. This implementation does nothing.</p>
     */
    public SoftwareContestSubmissionsAction() {
        this.viewData = new SoftwareContestSubmissionsDTO();
        this.formData = new ProjectIdForm(this.viewData);
    }

    /**
     * <p>Gets the form data.</p>
     *
     * @return an <code>ProjectIdForm</code> providing the data for form submitted by user.
     */
    public ProjectIdForm getFormData() {
        return this.formData;
    }

    /**
     * <p>Gets the current session associated with the incoming request from client.</p>
     *
     * @return a <code>SessionData</code> providing access to current session.
     */
    public SessionData getSessionData() {
        return this.sessionData;
    }

    /**
     * <p>Gets the data to be displayed by view mapped to this action.</p>
     *
     * @return a <code>SoftwareContestSubmissionsDTO</code> providing the collector for data to be rendered by the view
     *         mapped to this action.
     */
    public SoftwareContestSubmissionsDTO getViewData() {
        return this.viewData;
    }

    /**
     * Overrides the {@link #execute()} to check redirectToMilestone, if true, return the result 'milestone' to redirect
     * to milestone submissions page.
     *
     * @return the result code
     * @throws Exception if an unexpected error occurs.
     * @since 1.1
     */
    @Override
    public String execute() throws Exception {
        String result = super.execute();
        if (SUCCESS.equals(result)) {
            if (this.redirectToMilestone) {
                return "milestone";
            }
        }
        return result;
    }

    /**
     * <p>Handles the incoming request. Retrieves the list of submissions for requested contest and binds it to view
     * data along with other necessary details.</p>
     *
     * <p>
     *  Updates in version 1.1:
     *  - adds codes to get milestone submissions data if the round type is milestone round.
     * </p>
     *
     * @throws Exception if an unexpected error occurs.
     */
    @Override
    public void executeAction() throws Exception {
        getFormData().setProjectId(getProjectId());

        // Get current session
        HttpServletRequest request = DirectUtils.getServletRequest();
        this.sessionData = new SessionData(request.getSession());

        // Get current user
        TCSubject currentUser = DirectUtils.getTCSubjectFromSession();

        SoftwareCompetition softwareCompetition 
            = getContestServiceFacade().getSoftwareContestByProjectId(currentUser, getFormData().getProjectId());

        boolean hasMilestoneRound = DirectUtils.isMultiRound(softwareCompetition);

        // get the round type
        ContestRoundType roundType = getRoundType();

        // if round type is not specified, default to FINAL
        if (roundType == null) {
            roundType = ContestRoundType.FINAL;
        }

        if (hasMilestoneRound) {
            if (roundType == ContestRoundType.FINAL) {
                boolean isMilestoneRoundConfirmed 
                    = DirectUtils.getContestCheckout(softwareCompetition, ContestRoundType.MILESTONE);
                if (!isMilestoneRoundConfirmed) {
                    // if the milestone is not confirmed, redirect to milestone submission page
                    this.redirectToMilestone = true;
                    return;
                }
            }
        }

        if (roundType == ContestRoundType.FINAL) {
            // Set submissions, winners, reviewers data
            DataProvider.setSoftwareSubmissionsData(getViewData());
        } else {
            DataProvider.setSoftwareMilestoneSubmissionsData(getViewData());
        }

        // For normal request flow prepare various data to be displayed to user
        // Set contest stats
        ContestStatsDTO contestStats = DirectUtils.getContestStats(getCurrentUser(), getProjectId());
        getViewData().setContestStats(contestStats);

        // Set projects data
        List<ProjectBriefDTO> projects = DataProvider.getUserProjects(currentUser.getUserId());
        UserProjectsDTO userProjectsDTO = new UserProjectsDTO();
        userProjectsDTO.setProjects(projects);
        getViewData().setUserProjects(userProjectsDTO);

        // Set current project contests
        List<TypedContestBriefDTO> contests = DataProvider.getProjectTypedContests(
                currentUser.getUserId(), contestStats.getContest().getProject().getId());
        getSessionData().setCurrentProjectContests(contests);

        // Set current project context based on selected contest
        getSessionData().setCurrentProjectContext(contestStats.getContest().getProject());

        // set whether to show spec review
        viewData.setShowSpecReview(getSpecificationReviewService()
                .getSpecificationReview(currentUser, getProjectId()) != null);

        DirectUtils.setDashboardData(currentUser, getProjectId(), viewData,
                getContestServiceFacade(), true);
        
        // Determine if user can perform approval
        boolean approvalPhaseIsOpen = DirectUtils.isPhaseOpen(softwareCompetition, PhaseType.APPROVAL_PHASE);
        boolean userHasWriteFullPermission = DirectUtils.hasWritePermission(this, currentUser, getProjectId(), false);
        Resource approverResource = DirectUtils.getUserResourceByRole(currentUser.getUserId(), softwareCompetition,
                                                                      RESOURCE_ROLE_APPROVER_ID);
        boolean isApprovalCommitted = false;
        if (approverResource != null) {
            isApprovalCommitted = DirectUtils.hasReview(getProjectServices(), softwareCompetition,
                                                        PhaseType.APPROVAL_PHASE.getId(),
                                                        SCORECARD_TYPE_APPROVAL_ID, approverResource.getId());
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Context for approval by user " + currentUser.getUserId() + " for project " + getProjectId() 
                         + ": approvalPhaseIsOpen = " + approvalPhaseIsOpen + ", userHasWriteFullPermission = " 
                         + userHasWriteFullPermission + ", approverResource = " + approverResource 
                         + ", isApprovalCommitted = " + isApprovalCommitted);
        }

        getViewData().setShowApproval((approvalPhaseIsOpen || isApprovalCommitted) && userHasWriteFullPermission);
        getViewData().setApprovalCommitted(isApprovalCommitted);

        // add final fixes of the contest if exist
        setFinalFixes(DataProvider.getContestFinalFixes(getProjectId()));
    }

    /**
     * Checks whether all the submission are reviewed.
     *
     * @return true if all submissions are review, false otherwise.
     */
    public boolean isAllSubmissionReviewed() {
        final List<SoftwareSubmissionDTO> submissions = getViewData().getSubmissions();

        for(SoftwareSubmissionDTO s : submissions) {
            if (s.getReviews() == null || s.getReviews().size() == 0) {
                return false;
            }
        }

        return true;
    }
}
