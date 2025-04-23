import type {
  OpenAPIClient,
  Parameters,
  UnknownParamsObject,
  OperationResponse,
  AxiosRequestConfig,
} from 'openapi-client-axios';

declare namespace Components {
    namespace Schemas {
        /**
         * An assignment in a course
         */
        export interface Assignment {
            /**
             * example:
             * 999-9999-9999-99
             */
            id?: string;
            /**
             * example:
             * Assessment 1
             */
            name: string;
            /**
             * example:
             * Quiz
             */
            category: string;
            /**
             * example:
             * 1245678
             */
            canvas_id: number; // int64
            /**
             * example:
             * 15
             */
            points: number; // double
            /**
             * example:
             * Gradescope
             */
            external_service?: string;
            /**
             * example:
             * 14
             */
            external_points?: number;
            /**
             * example:
             * 2020-01-15T12:00:00.000Z
             */
            due_date: string; // date-time
            /**
             * example:
             * 2020-01-01T12:00:00.000Z
             */
            unlock_date: string; // date-time
            /**
             * example:
             * true
             */
            enabled: boolean;
            /**
             * example:
             * false
             */
            group_assignment?: boolean;
            /**
             * example:
             * false
             */
            attention_required?: boolean;
            /**
             * example:
             * false
             */
            frozen?: boolean;
        }
        /**
         * An slim assignment in a course
         */
        export interface AssignmentSlim {
            /**
             * example:
             * 999-9999-9999-99
             */
            id: string;
            /**
             * example:
             * Assessment 1
             */
            name: string;
            /**
             * example:
             * 15
             */
            points: number; // double
            /**
             * example:
             * 2020-01-15T12:00:00.000Z
             */
            due_date: string; // date-time
            /**
             * example:
             * 2020-01-01T12:00:00.000Z
             */
            unlock_date: string; // date-time
            /**
             * example:
             * Quiz
             */
            category: string;
        }
        /**
         * A complete course
         */
        export interface Course {
            /**
             * example:
             * 999-9999-9999-99
             */
            id?: string;
            /**
             * example:
             * Fall 2020
             */
            term: string;
            /**
             * example:
             * true
             */
            enabled: boolean;
            /**
             * example:
             * EXCL101
             */
            name: string;
            /**
             * example:
             * Fall.2020.EXCL.101
             */
            code: string;
            /**
             * example:
             * 123456
             */
            canvas_id: number; // int64
            /**
             * example:
             * 932552
             */
            gradescope_id?: string;
            members?: /* A user in a course */ CourseMember[];
            assignments?: /* An assignment in a course */ Assignment[];
            /**
             * example:
             * [
             *   "fall.2020.excl.101.section.a"
             * ]
             */
            sections?: string[];
            late_request_config: /* The late request config for a course */ CourseLateRequestConfig;
        }
        /**
         * The late request config for a course
         */
        export interface CourseLateRequestConfig {
            /**
             * example:
             * true
             */
            late_passes_enabled: boolean;
            /**
             * example:
             * [
             *   "illness",
             *   "excused absence"
             * ]
             */
            enabled_extension_reasons: string[];
            /**
             * example:
             * 5
             */
            total_late_passes_allowed: number;
            /**
             * example:
             * Late Passes
             */
            late_pass_name: string;
        }
        /**
         * A user in a course
         */
        export interface CourseMember {
            /**
             * example:
             * 99999999
             */
            cwid: string;
            /**
             * example:
             * self
             */
            canvas_id: string;
            /**
             * example:
             * owner
             */
            course_role: "student" | "instructor" | "ta" | "owner";
            sections?: string[];
            /**
             * example:
             * 3
             */
            late_passes_used?: number; // double
        }
        /**
         * A slim course
         */
        export interface CourseSlim {
            /**
             * example:
             * 999-9999-9999-99
             */
            id?: string;
            /**
             * example:
             * Fall 2020
             */
            term: string;
            /**
             * example:
             * EXCL101
             */
            name: string;
            /**
             * example:
             * Fall.2020.EXCL.101
             */
            code: string;
        }
        /**
         * Import a new course from canvas
         */
        export interface CourseSyncTask {
            /**
             * example:
             * 999999
             */
            canvas_id: number; // int64
            /**
             * example:
             * false
             */
            overwrite_name?: boolean;
            /**
             * example:
             * false
             */
            overwrite_code?: boolean;
            /**
             * example:
             * false
             */
            import_users?: boolean;
            /**
             * example:
             * false
             */
            import_assignments?: boolean;
        }
        /**
         * A credential for external service
         */
        export interface Credential {
            /**
             * example:
             * 99-9999-9999-99
             */
            id?: string;
            /**
             * example:
             * Cred 1
             */
            name?: string;
            /**
             * example:
             * canvas
             */
            service: "canvas" | "gradescope";
            /**
             * example:
             * a super secure key
             */
            api_key?: string;
            /**
             * example:
             * true
             */
            private?: boolean;
            owning_user: /* A server user */ User;
        }
        /**
         * A slim course and a CourseMember
         */
        export interface Enrollment {
            /**
             * example:
             * 999-9999-9999-99
             */
            id?: string;
            /**
             * example:
             * Fall 2020
             */
            term: string;
            /**
             * example:
             * EXCL101
             */
            name: string;
            /**
             * example:
             * Fall.2020.EXCL.101
             */
            code: string;
            /**
             * example:
             * 99999999
             */
            cwid: string;
            /**
             * example:
             * owner
             */
            course_role: "student" | "instructor" | "ta" | "owner";
        }
        /**
         * An error occurred while processing that query
         */
        export interface ErrorResponse {
            error_source?: string;
            error_message?: string;
        }
        /**
         * An extension for an assignment
         */
        export interface Extension {
            /**
             * example:
             * 999-9999-9999-99
             */
            id?: string;
            /**
             * example:
             * Tech Issues
             */
            reason: string;
            user_reviewer?: /* A user in a course */ CourseMember;
            /**
             * example:
             * 2020-01-01T12:00:00.000Z
             */
            response_timestamp?: string; // date-time
            /**
             * example:
             * Some comment about the work
             */
            comments: string;
            /**
             * example:
             * Your 3 day extension for illness is approved
             */
            response_to_requester?: string;
        }
        /**
         * A generic request for extending work deadlines
         */
        export interface LateRequest {
            /**
             * example:
             * 999-9999-9999-99
             */
            id?: string;
            /**
             * example:
             * 999-9999-9999-99
             */
            assignment_id?: string;
            /**
             * example:
             * 999-9999-9999-99
             */
            assignment_name?: string;
            /**
             * example:
             * 2020-01-01T12:00:00.000Z
             */
            date_submitted: string; // date-time
            /**
             * example:
             * 2
             */
            num_days_requested: number; // double
            extension?: /* An extension for an assignment */ Extension;
            /**
             * example:
             * 999-9999-9999-99
             */
            user_requester_id?: string;
            /**
             * example:
             * Extension pending instructor approval
             */
            status: "pending" | "approved" | "rejected";
            /**
             * example:
             * Late Pass
             */
            request_type: "extension" | "late_pass";
        }
        /**
         * The master migration that contains to the list of migration objects
         */
        export interface MasterMigration {
            /**
             * example:
             * 999-9999-9999-99
             */
            id?: string;
            migrator?: /* A server user */ User;
            /**
             * example:
             * 2020-01-01T12:00:00.000Z
             */
            date_started?: string; // date-time
            migrations?: /* Migration object that has a single assignment and a policy */ Migration[];
            stats?: /* The statistics from a master migration, has the number of: extensions, late penalties, missing, no credit */ MasterMigrationStatistics;
        }
        /**
         * The statistics from a master migration, has the number of: extensions, late penalties, missing, no credit
         */
        export interface MasterMigrationStatistics {
            /**
             * example:
             * 2
             */
            total_submission: number;
            /**
             * example:
             * 12
             */
            late_requests: number;
            /**
             * example:
             * 1
             */
            total_extensions: number;
            /**
             * example:
             * 5
             */
            total_late_passes: number;
            /**
             * example:
             * 3
             */
            unapproved_requests: number;
        }
        /**
         * Migration object that has a single assignment and a policy
         */
        export interface Migration {
            assignment: /* An assignment in a course */ Assignment;
            policy: /* A grading policy */ Policy;
        }
        /**
         * Create a new policy file
         */
        export interface NewPolicy {
            /**
             * example:
             * Default Policy
             */
            name: string;
            /**
             * example:
             * default.js
             */
            file_path: string;
            /**
             * example:
             * The default policy
             *
             */
            description?: string;
            /**
             * example:
             * // valid javascript code
             *
             */
            file_data: string; // binary
        }
        /**
         * A grading policy
         */
        export interface Policy {
            /**
             * example:
             * 999-9999-9999-99
             */
            id?: string;
            /**
             * example:
             * Default Policy
             */
            name?: string;
            /**
             * example:
             * the default grading policy :)
             *
             */
            description?: string;
            /**
             * example:
             * https://s3.aws.com/999-9999-9999-99/default.js
             */
            uri?: string;
            /**
             * example:
             * 1
             */
            number_of_migrations?: number;
        }
        /**
         * Information relevant to a student
         */
        export interface StudentInformation {
            /**
             * example:
             * student
             */
            course_role: "student" | "instructor" | "ta" | "owner";
            /**
             * example:
             * Jane Doe
             */
            professor: string;
            /**
             * example:
             * 3
             */
            late_passes_used?: number; // double
            course: /* A complete course */ Course;
        }
        /**
         * An async task on the server
         */
        export interface Task {
            /**
             * example:
             * 291
             */
            id: number; // int64
            /**
             * example:
             * Course Import - Canvas
             */
            name?: string;
            /**
             * example:
             * 2020-01-01T12:00:00.000Z
             */
            submitted_time: string; // date-time
            /**
             * example:
             * 2020-01-01T12:00:00.000Z
             */
            completed_time?: string; // date-time
            /**
             * example:
             * COMPLETED
             */
            status: string;
            /**
             * example:
             * A message about the status of the task
             */
            message?: string;
        }
        /**
         * A server user
         */
        export interface User {
            /**
             * example:
             * user@test.com
             */
            email: string; // email
            /**
             * example:
             * 99999999
             */
            cwid: string;
            /**
             * example:
             * Test User
             */
            name: string;
            /**
             * example:
             * false
             */
            admin: boolean;
            /**
             * example:
             * true
             */
            enabled?: boolean;
        }
    }
}
declare namespace Paths {
    namespace AddAssignment {
        namespace Parameters {
            /**
             * example:
             * 999-9999-9999-99
             */
            export type CourseId = string;
        }
        export interface PathParameters {
            course_id: /**
             * example:
             * 999-9999-9999-99
             */
            Parameters.CourseId;
        }
        export type RequestBody = /* An assignment in a course */ Components.Schemas.Assignment;
        namespace Responses {
            export type $201 = /* An assignment in a course */ Components.Schemas.Assignment;
            export type $404 = /* An error occurred while processing that query */ Components.Schemas.ErrorResponse;
        }
    }
    namespace AddCourseMember {
        namespace Parameters {
            /**
             * example:
             * 999-9999-9999-99
             */
            export type CourseId = string;
        }
        export interface PathParameters {
            course_id: /**
             * example:
             * 999-9999-9999-99
             */
            Parameters.CourseId;
        }
        export type RequestBody = /* A user in a course */ Components.Schemas.CourseMember;
        namespace Responses {
            export interface $201 {
            }
            export type $404 = /* An error occurred while processing that query */ Components.Schemas.ErrorResponse;
        }
    }
    namespace AdminGetAllPolicies {
        namespace Parameters {
            /**
             * example:
             * 999-9999-9999-99
             */
            export type CourseId = string;
        }
        export interface PathParameters {
            course_id: /**
             * example:
             * 999-9999-9999-99
             */
            Parameters.CourseId;
        }
        namespace Responses {
            export type $200 = /* A grading policy */ Components.Schemas.Policy[];
        }
    }
    namespace AdminUpdateUser {
        export type RequestBody = /* A server user */ Components.Schemas.User;
        namespace Responses {
            export type $202 = /* A server user */ Components.Schemas.User;
        }
    }
    namespace ApproveExtension {
        namespace Parameters {
            /**
             * example:
             * 99-9999-9999-99
             */
            export type AssignmentId = string;
            /**
             * example:
             * 99-9999-9999-99
             */
            export type CourseId = string;
            /**
             * example:
             * 99-9999-9999-99
             */
            export type ExtensionId = string;
            /**
             * example:
             * 11111111
             */
            export type UserId = string;
        }
        export interface PathParameters {
            course_id: /**
             * example:
             * 99-9999-9999-99
             */
            Parameters.CourseId;
            assignment_id: /**
             * example:
             * 99-9999-9999-99
             */
            Parameters.AssignmentId;
            user_id: /**
             * example:
             * 11111111
             */
            Parameters.UserId;
            extension_id: /**
             * example:
             * 99-9999-9999-99
             */
            Parameters.ExtensionId;
        }
        namespace Responses {
            export type $202 = /* A generic request for extending work deadlines */ Components.Schemas.LateRequest;
            export type $400 = /* An error occurred while processing that query */ Components.Schemas.ErrorResponse;
            export type $404 = /* An error occurred while processing that query */ Components.Schemas.ErrorResponse;
        }
    }
    namespace CheckHealth {
        namespace Responses {
            export interface $200 {
            }
            export interface $500 {
            }
        }
    }
    namespace CreateExtensionRequest {
        namespace Parameters {
            /**
             * example:
             * 999-9999-9999-99
             */
            export type CourseId = string;
        }
        export interface PathParameters {
            course_id: /**
             * example:
             * 999-9999-9999-99
             */
            Parameters.CourseId;
        }
        export type RequestBody = /* A generic request for extending work deadlines */ Components.Schemas.LateRequest;
        namespace Responses {
            export type $201 = /* A generic request for extending work deadlines */ Components.Schemas.LateRequest;
            export type $404 = /* An error occurred while processing that query */ Components.Schemas.ErrorResponse;
        }
    }
    namespace CreateMasterMigration {
        namespace Parameters {
            /**
             * example:
             * 999-9999-9999-99
             */
            export type CourseId = string;
        }
        export interface PathParameters {
            course_id: /**
             * example:
             * 999-9999-9999-99
             */
            Parameters.CourseId;
        }
        export type RequestBody = /* The master migration that contains to the list of migration objects */ Components.Schemas.MasterMigration;
        namespace Responses {
            export type $201 = /* The master migration that contains to the list of migration objects */ Components.Schemas.MasterMigration;
            export type $404 = /* An error occurred while processing that query */ Components.Schemas.ErrorResponse;
        }
    }
    namespace CreateMigrationForMasterMigration {
        namespace Parameters {
            /**
             * example:
             * 999-9999-9999-99
             */
            export type CourseId = string;
            /**
             * example:
             * 999-9999-9999-99
             */
            export type MasterMigrationId = string;
        }
        export interface PathParameters {
            course_id: /**
             * example:
             * 999-9999-9999-99
             */
            Parameters.CourseId;
            master_migration_id: /**
             * example:
             * 999-9999-9999-99
             */
            Parameters.MasterMigrationId;
        }
        export type RequestBody = /* Migration object that has a single assignment and a policy */ Components.Schemas.Migration;
        namespace Responses {
            export type $201 = /* The master migration that contains to the list of migration objects */ Components.Schemas.MasterMigration;
            export type $404 = /* An error occurred while processing that query */ Components.Schemas.ErrorResponse;
        }
    }
    namespace CreateUser {
        export type RequestBody = /* A server user */ Components.Schemas.User;
        namespace Responses {
            export interface $201 {
            }
            export type $400 = /* An error occurred while processing that query */ Components.Schemas.ErrorResponse;
        }
    }
    namespace DeleteCredential {
        namespace Parameters {
            /**
             * example:
             * 99-9999-9999-99
             */
            export type CredentialId = string;
        }
        export interface PathParameters {
            credential_id: /**
             * example:
             * 99-9999-9999-99
             */
            Parameters.CredentialId;
        }
        namespace Responses {
            export interface $204 {
            }
            export type $400 = /* An error occurred while processing that query */ Components.Schemas.ErrorResponse;
            export type $404 = /* An error occurred while processing that query */ Components.Schemas.ErrorResponse;
        }
    }
    namespace DeletePolicy {
        namespace Parameters {
            /**
             * example:
             * 999-9999-9999-99
             */
            export type CourseId = string;
            /**
             * example:
             * 999-9999-9999-99
             */
            export type PolicyId = string;
        }
        export interface PathParameters {
            course_id: /**
             * example:
             * 999-9999-9999-99
             */
            Parameters.CourseId;
            policy_id: /**
             * example:
             * 999-9999-9999-99
             */
            Parameters.PolicyId;
        }
        namespace Responses {
            export interface $204 {
            }
        }
    }
    namespace DenyExtension {
        namespace Parameters {
            /**
             * example:
             * 99-9999-9999-99
             */
            export type AssignmentId = string;
            /**
             * example:
             * 99-9999-9999-99
             */
            export type CourseId = string;
            /**
             * example:
             * 99-9999-9999-99
             */
            export type ExtensionId = string;
            /**
             * example:
             * Bad extension
             */
            export type Reason = string;
            /**
             * example:
             * 11111111
             */
            export type UserId = string;
        }
        export interface PathParameters {
            course_id: /**
             * example:
             * 99-9999-9999-99
             */
            Parameters.CourseId;
            assignment_id: /**
             * example:
             * 99-9999-9999-99
             */
            Parameters.AssignmentId;
            user_id: /**
             * example:
             * 11111111
             */
            Parameters.UserId;
            extension_id: /**
             * example:
             * 99-9999-9999-99
             */
            Parameters.ExtensionId;
        }
        export interface QueryParameters {
            reason: /**
             * example:
             * Bad extension
             */
            Parameters.Reason;
        }
        namespace Responses {
            export type $202 = /* A generic request for extending work deadlines */ Components.Schemas.LateRequest;
            export type $400 = /* An error occurred while processing that query */ Components.Schemas.ErrorResponse;
            export type $404 = /* An error occurred while processing that query */ Components.Schemas.ErrorResponse;
        }
    }
    namespace DisableAssignment {
        namespace Parameters {
            /**
             * example:
             * 999-9999-9999-99
             */
            export type AssignmentId = string;
            /**
             * example:
             * 999-9999-9999-99
             */
            export type CourseId = string;
        }
        export interface PathParameters {
            course_id: /**
             * example:
             * 999-9999-9999-99
             */
            Parameters.CourseId;
            assignment_id: /**
             * example:
             * 999-9999-9999-99
             */
            Parameters.AssignmentId;
        }
        namespace Responses {
            export interface $200 {
            }
            export type $404 = /* An error occurred while processing that query */ Components.Schemas.ErrorResponse;
        }
    }
    namespace DisableUser {
        namespace Parameters {
            /**
             * example:
             * 99999999
             */
            export type Cwid = string;
        }
        export interface PathParameters {
            cwid: /**
             * example:
             * 99999999
             */
            Parameters.Cwid;
        }
        namespace Responses {
            export interface $201 {
            }
            export type $404 = /* An error occurred while processing that query */ Components.Schemas.ErrorResponse;
        }
    }
    namespace EnableAssignment {
        namespace Parameters {
            /**
             * example:
             * 999-9999-9999-99
             */
            export type AssignmentId = string;
            /**
             * example:
             * 999-9999-9999-99
             */
            export type CourseId = string;
        }
        export interface PathParameters {
            course_id: /**
             * example:
             * 999-9999-9999-99
             */
            Parameters.CourseId;
            assignment_id: /**
             * example:
             * 999-9999-9999-99
             */
            Parameters.AssignmentId;
        }
        namespace Responses {
            export interface $200 {
            }
            export type $404 = /* An error occurred while processing that query */ Components.Schemas.ErrorResponse;
        }
    }
    namespace EnableUser {
        namespace Parameters {
            /**
             * example:
             * 99999999
             */
            export type Cwid = string;
        }
        export interface PathParameters {
            cwid: /**
             * example:
             * 99999999
             */
            Parameters.Cwid;
        }
        namespace Responses {
            export interface $201 {
            }
            export type $404 = /* An error occurred while processing that query */ Components.Schemas.ErrorResponse;
        }
    }
    namespace GetAllApprovedExtensionsForAssignment {
        namespace Parameters {
            /**
             * example:
             * 999-9999-9999-99
             */
            export type AssignmentId = string;
            /**
             * example:
             * 999-9999-9999-99
             */
            export type CourseId = string;
            /**
             * example:
             * 999-9999-9999-99
             */
            export type ExtensionId = string;
            export type Status = "approved" | "denied" | "pending" | "all";
        }
        export interface PathParameters {
            course_id: /**
             * example:
             * 999-9999-9999-99
             */
            Parameters.CourseId;
            assignment_id: /**
             * example:
             * 999-9999-9999-99
             */
            Parameters.AssignmentId;
            extension_id: /**
             * example:
             * 999-9999-9999-99
             */
            Parameters.ExtensionId;
        }
        export interface QueryParameters {
            status?: Parameters.Status;
        }
        namespace Responses {
            export type $200 = /* An error occurred while processing that query */ Components.Schemas.ErrorResponse[];
            export type $404 = /* An error occurred while processing that query */ Components.Schemas.ErrorResponse;
        }
    }
    namespace GetAllApprovedExtensionsForMember {
        namespace Parameters {
            /**
             * example:
             * 999-9999-9999-99
             */
            export type CourseId = string;
            /**
             * example:
             * 999-9999-9999-99
             */
            export type ExtensionId = string;
            export type Status = "approved" | "denied" | "pending";
            /**
             * example:
             * 11111111
             */
            export type UserId = string;
        }
        export interface PathParameters {
            course_id: /**
             * example:
             * 999-9999-9999-99
             */
            Parameters.CourseId;
            user_id: /**
             * example:
             * 11111111
             */
            Parameters.UserId;
            extension_id: /**
             * example:
             * 999-9999-9999-99
             */
            Parameters.ExtensionId;
        }
        export interface QueryParameters {
            status?: Parameters.Status;
        }
        namespace Responses {
            export type $200 = /* An error occurred while processing that query */ Components.Schemas.ErrorResponse[];
            export type $404 = /* An error occurred while processing that query */ Components.Schemas.ErrorResponse;
        }
    }
    namespace GetAllExtensions {
        namespace Parameters {
            /**
             * example:
             * 999-9999-9999-99
             */
            export type CourseId = string;
        }
        export interface PathParameters {
            course_id: /**
             * example:
             * 999-9999-9999-99
             */
            Parameters.CourseId;
        }
        namespace Responses {
            export type $200 = /* A generic request for extending work deadlines */ Components.Schemas.LateRequest[];
            export type $404 = /* An error occurred while processing that query */ Components.Schemas.ErrorResponse;
        }
    }
    namespace GetAllExtensionsForCourse {
        namespace Parameters {
            /**
             * example:
             * 999-9999-9999-99
             */
            export type CourseId = string;
            export type Status = "approved" | "denied" | "pending";
        }
        export interface PathParameters {
            course_id: /**
             * example:
             * 999-9999-9999-99
             */
            Parameters.CourseId;
        }
        export interface QueryParameters {
            status?: Parameters.Status;
        }
        namespace Responses {
            export type $200 = /* A generic request for extending work deadlines */ Components.Schemas.LateRequest[];
            export type $404 = /* An error occurred while processing that query */ Components.Schemas.ErrorResponse;
        }
    }
    namespace GetAllExtensionsForSection {
        namespace Parameters {
            /**
             * example:
             * 999-9999-9999-99
             */
            export type CourseId = string;
            /**
             * example:
             * 999-9999-9999-99
             */
            export type ExtensionId = string;
            /**
             * example:
             * Section F
             */
            export type SectionId = string;
            export type Status = "approved" | "denied" | "pending";
        }
        export interface PathParameters {
            course_id: /**
             * example:
             * 999-9999-9999-99
             */
            Parameters.CourseId;
            section_id: /**
             * example:
             * Section F
             */
            Parameters.SectionId;
            extension_id: /**
             * example:
             * 999-9999-9999-99
             */
            Parameters.ExtensionId;
        }
        export interface QueryParameters {
            status?: Parameters.Status;
        }
        namespace Responses {
            export type $200 = /* An error occurred while processing that query */ Components.Schemas.ErrorResponse[];
            export type $404 = /* An error occurred while processing that query */ Components.Schemas.ErrorResponse;
        }
    }
    namespace GetAllMasterMigrationsForCourse {
        namespace Parameters {
            /**
             * example:
             * 999-9999-9999-99
             */
            export type CourseId = string;
        }
        export interface PathParameters {
            course_id: /**
             * example:
             * 999-9999-9999-99
             */
            Parameters.CourseId;
        }
        namespace Responses {
            export type $200 = /* The master migration that contains to the list of migration objects */ Components.Schemas.MasterMigration[];
            export type $404 = /* An error occurred while processing that query */ Components.Schemas.ErrorResponse;
        }
    }
    namespace GetAllPolicies {
        namespace Parameters {
            /**
             * example:
             * 999-9999-9999-99
             */
            export type CourseId = string;
        }
        export interface PathParameters {
            course_id: /**
             * example:
             * 999-9999-9999-99
             */
            Parameters.CourseId;
        }
        namespace Responses {
            export type $200 = /* A grading policy */ Components.Schemas.Policy[];
        }
    }
    namespace GetAllTasksForUser {
        namespace Responses {
            export type $200 = /* An async task on the server */ Components.Schemas.Task[];
        }
    }
    namespace GetAllUsers {
        namespace Responses {
            export type $200 = /* A server user */ Components.Schemas.User[];
        }
    }
    namespace GetCourse {
        namespace Parameters {
            /**
             * example:
             * 999-9999-9999-99
             */
            export type CourseId = string;
            export type Include = ("members" | "assignments" | "sections")[];
        }
        export interface PathParameters {
            course_id: /**
             * example:
             * 999-9999-9999-99
             */
            Parameters.CourseId;
        }
        export interface QueryParameters {
            include?: Parameters.Include;
        }
        namespace Responses {
            export type $200 = /* A complete course */ Components.Schemas.Course;
            export type $404 = /* An error occurred while processing that query */ Components.Schemas.ErrorResponse;
        }
    }
    namespace GetCourseAssignmentsStudent {
        namespace Parameters {
            /**
             * example:
             * 999-9999-9999-99
             */
            export type CourseId = string;
        }
        export interface PathParameters {
            course_id: /**
             * example:
             * 999-9999-9999-99
             */
            Parameters.CourseId;
        }
        namespace Responses {
            export type $200 = /* An slim assignment in a course */ Components.Schemas.AssignmentSlim[];
        }
    }
    namespace GetCourseInformationInstructor {
        namespace Parameters {
            /**
             * example:
             * 999-9999-9999-99
             */
            export type CourseId = string;
        }
        export interface PathParameters {
            course_id: /**
             * example:
             * 999-9999-9999-99
             */
            Parameters.CourseId;
        }
        namespace Responses {
            export type $200 = /* A complete course */ Components.Schemas.Course;
        }
    }
    namespace GetCourseInformationStudent {
        namespace Parameters {
            /**
             * example:
             * 999-9999-9999-99
             */
            export type CourseId = string;
        }
        export interface PathParameters {
            course_id: /**
             * example:
             * 999-9999-9999-99
             */
            Parameters.CourseId;
        }
        namespace Responses {
            export type $200 = /* Information relevant to a student */ Components.Schemas.StudentInformation;
        }
    }
    namespace GetCourses {
        namespace Parameters {
            /**
             * example:
             * true
             */
            export type OnlyActive = boolean;
        }
        export interface QueryParameters {
            onlyActive?: /**
             * example:
             * true
             */
            Parameters.OnlyActive;
        }
        namespace Responses {
            export type $200 = /* A complete course */ Components.Schemas.Course[];
        }
    }
    namespace GetCoursesStudent {
        namespace Responses {
            export type $200 = /* A slim course */ Components.Schemas.CourseSlim[];
        }
    }
    namespace GetCredentials {
        namespace Responses {
            export type $200 = /* A credential for external service */ Components.Schemas.Credential[];
            export type $404 = /* An error occurred while processing that query */ Components.Schemas.ErrorResponse;
        }
    }
    namespace GetEnrollments {
        namespace Responses {
            export type $200 = /* A slim course and a CourseMember */ Components.Schemas.Enrollment[];
            export type $404 = /* An error occurred while processing that query */ Components.Schemas.ErrorResponse;
        }
    }
    namespace GetInstructorEnrollments {
        namespace Parameters {
            /**
             * example:
             * 999-9999-9999-99
             */
            export type CourseId = string;
            /**
             * Part of (or full) cwid to search for
             * example:
             * 9999999
             */
            export type Cwid = string;
            /**
             * Part of (or full) name to search for
             * example:
             * Alex
             */
            export type Name = string;
        }
        export interface PathParameters {
            course_id: /**
             * example:
             * 999-9999-9999-99
             */
            Parameters.CourseId;
        }
        export interface QueryParameters {
            name?: /**
             * Part of (or full) name to search for
             * example:
             * Alex
             */
            Parameters.Name;
            cwid?: /**
             * Part of (or full) cwid to search for
             * example:
             * 9999999
             */
            Parameters.Cwid;
        }
        namespace Responses {
            export type $200 = /* A user in a course */ Components.Schemas.CourseMember[];
            export type $404 = /* An error occurred while processing that query */ Components.Schemas.ErrorResponse;
        }
    }
    namespace GetMembers {
        namespace Parameters {
            /**
             * example:
             * 999-9999-9999-99
             */
            export type CourseId = string;
            /**
             * Part of (or full) cwid to search for
             * example:
             * 9999999
             */
            export type Cwid = string;
            export type Enrollments = ("tas" | "instructors" | "students")[];
            /**
             * Part of (or full) name to search for
             * example:
             * Alex
             */
            export type Name = string;
        }
        export interface PathParameters {
            course_id: /**
             * example:
             * 999-9999-9999-99
             */
            Parameters.CourseId;
        }
        export interface QueryParameters {
            enrollments?: Parameters.Enrollments;
            name?: /**
             * Part of (or full) name to search for
             * example:
             * Alex
             */
            Parameters.Name;
            cwid?: /**
             * Part of (or full) cwid to search for
             * example:
             * 9999999
             */
            Parameters.Cwid;
        }
        namespace Responses {
            export type $200 = /* A user in a course */ Components.Schemas.CourseMember[];
            export type $404 = /* An error occurred while processing that query */ Components.Schemas.ErrorResponse;
        }
    }
    namespace GetTask {
        namespace Parameters {
            /**
             * example:
             * 219
             */
            export type TaskId = number; // int64
        }
        export interface PathParameters {
            task_id: /**
             * example:
             * 219
             */
            Parameters.TaskId /* int64 */;
        }
        namespace Responses {
            export type $200 = /* An async task on the server */ Components.Schemas.Task;
            export type $404 = /* An error occurred while processing that query */ Components.Schemas.ErrorResponse;
        }
    }
    namespace GetUser {
        namespace Responses {
            export type $200 = /* A server user */ Components.Schemas.User;
        }
    }
    namespace ImportCourse {
        namespace Parameters {
            /**
             * example:
             * 999-9999-9999-99
             */
            export type CourseId = string;
        }
        export interface PathParameters {
            course_id: /**
             * example:
             * 999-9999-9999-99
             */
            Parameters.CourseId;
        }
        export type RequestBody = /* Import a new course from canvas */ Components.Schemas.CourseSyncTask;
        namespace Responses {
            export type $202 = /* An async task on the server */ Components.Schemas.Task[];
            export type $403 = /* An error occurred while processing that query */ Components.Schemas.ErrorResponse;
            export type $404 = /* An error occurred while processing that query */ Components.Schemas.ErrorResponse;
        }
    }
    namespace MarkCredentialAsPrivate {
        namespace Parameters {
            /**
             * example:
             * 99-9999-9999-99
             */
            export type CredentialId = string;
        }
        export interface PathParameters {
            credential_id: /**
             * example:
             * 99-9999-9999-99
             */
            Parameters.CredentialId;
        }
        namespace Responses {
            export interface $202 {
            }
            export type $400 = /* An error occurred while processing that query */ Components.Schemas.ErrorResponse;
            export type $404 = /* An error occurred while processing that query */ Components.Schemas.ErrorResponse;
        }
    }
    namespace MarkCredentialAsPublic {
        namespace Parameters {
            /**
             * example:
             * 99-9999-9999-99
             */
            export type CredentialId = string;
        }
        export interface PathParameters {
            credential_id: /**
             * example:
             * 99-9999-9999-99
             */
            Parameters.CredentialId;
        }
        namespace Responses {
            export interface $202 {
            }
            export type $400 = /* An error occurred while processing that query */ Components.Schemas.ErrorResponse;
            export type $404 = /* An error occurred while processing that query */ Components.Schemas.ErrorResponse;
        }
    }
    namespace NewCourse {
        export type RequestBody = /* A complete course */ Components.Schemas.Course;
        namespace Responses {
            export type $201 = /* A complete course */ Components.Schemas.Course;
        }
    }
    namespace NewCredential {
        export type RequestBody = /* A credential for external service */ Components.Schemas.Credential;
        namespace Responses {
            export type $202 = /* A credential for external service */ Components.Schemas.Credential;
            export type $400 = /* An error occurred while processing that query */ Components.Schemas.ErrorResponse;
            export type $404 = /* An error occurred while processing that query */ Components.Schemas.ErrorResponse;
        }
    }
    namespace NewExtension {
        namespace Parameters {
            /**
             * example:
             * 999-9999-9999-99
             */
            export type AssignmentId = string;
            /**
             * example:
             * 999-9999-9999-99
             */
            export type CourseId = string;
            /**
             * example:
             * 999-9999-9999-99
             */
            export type ExtensionId = string;
            /**
             * example:
             * 11111111
             */
            export type UserId = string;
        }
        export interface PathParameters {
            course_id: /**
             * example:
             * 999-9999-9999-99
             */
            Parameters.CourseId;
            assignment_id: /**
             * example:
             * 999-9999-9999-99
             */
            Parameters.AssignmentId;
            user_id: /**
             * example:
             * 11111111
             */
            Parameters.UserId;
            extension_id: /**
             * example:
             * 999-9999-9999-99
             */
            Parameters.ExtensionId;
        }
        namespace Responses {
            export type $201 = /* An extension for an assignment */ Components.Schemas.Extension;
            export type $404 = /* An error occurred while processing that query */ Components.Schemas.ErrorResponse;
        }
    }
    namespace NewPolicy {
        namespace Parameters {
            /**
             * example:
             * 999-9999-9999-99
             */
            export type CourseId = string;
        }
        export interface PathParameters {
            course_id: /**
             * example:
             * 999-9999-9999-99
             */
            Parameters.CourseId;
        }
        export type RequestBody = /* Create a new policy file */ Components.Schemas.NewPolicy;
        namespace Responses {
            export type $201 = /* A grading policy */ Components.Schemas.Policy;
        }
    }
    namespace SyncCourse {
        namespace Parameters {
            /**
             * example:
             * 999-9999-9999-99
             */
            export type CourseId = string;
        }
        export interface PathParameters {
            course_id: /**
             * example:
             * 999-9999-9999-99
             */
            Parameters.CourseId;
        }
        export type RequestBody = /* Import a new course from canvas */ Components.Schemas.CourseSyncTask;
        namespace Responses {
            export type $202 = /* An async task on the server */ Components.Schemas.Task[];
            export type $403 = /* An error occurred while processing that query */ Components.Schemas.ErrorResponse;
            export type $404 = /* An error occurred while processing that query */ Components.Schemas.ErrorResponse;
        }
    }
    namespace UpdateAssignment {
        namespace Parameters {
            /**
             * example:
             * 999-9999-9999-99
             */
            export type CourseId = string;
        }
        export interface PathParameters {
            course_id: /**
             * example:
             * 999-9999-9999-99
             */
            Parameters.CourseId;
        }
        export type RequestBody = /* An assignment in a course */ Components.Schemas.Assignment;
        namespace Responses {
            export interface $201 {
            }
            export type $404 = /* An error occurred while processing that query */ Components.Schemas.ErrorResponse;
        }
    }
    namespace UpdateCourse {
        namespace Parameters {
            /**
             * example:
             * 999-9999-9999-99
             */
            export type CourseId = string;
        }
        export interface PathParameters {
            course_id: /**
             * example:
             * 999-9999-9999-99
             */
            Parameters.CourseId;
        }
        export type RequestBody = /* A complete course */ Components.Schemas.Course;
        namespace Responses {
            export interface $204 {
            }
            export type $404 = /* An error occurred while processing that query */ Components.Schemas.ErrorResponse;
        }
    }
    namespace UpdateCourseMember {
        namespace Parameters {
            /**
             * example:
             * 999-9999-9999-99
             */
            export type CourseId = string;
        }
        export interface PathParameters {
            course_id: /**
             * example:
             * 999-9999-9999-99
             */
            Parameters.CourseId;
        }
        export type RequestBody = /* A user in a course */ Components.Schemas.CourseMember;
        namespace Responses {
            export interface $201 {
            }
            export type $404 = /* An error occurred while processing that query */ Components.Schemas.ErrorResponse;
        }
    }
    namespace UpdateGrade {
        namespace Parameters {
            /**
             * example:
             * 99-9999-9999-99
             */
            export type CourseId = string;
            /**
             * example:
             * 99-9999-9999-99
             */
            export type MasterMigrationId = string;
        }
        export interface PathParameters {
            course_id: /**
             * example:
             * 99-9999-9999-99
             */
            Parameters.CourseId;
            master_migration_id: /**
             * example:
             * 99-9999-9999-99
             */
            Parameters.MasterMigrationId;
        }
        namespace Responses {
            export interface $202 {
            }
            export type $400 = /* An error occurred while processing that query */ Components.Schemas.ErrorResponse;
            export type $404 = /* An error occurred while processing that query */ Components.Schemas.ErrorResponse;
        }
    }
    namespace UpdatePolicy {
        namespace Parameters {
            /**
             * example:
             * 99-9999-9999-99
             */
            export type AssignmentId = string;
            /**
             * example:
             * 99-9999-9999-99
             */
            export type CourseId = string;
            /**
             * example:
             * 99-9999-9999-99
             */
            export type MigrationId = string;
        }
        export interface PathParameters {
            course_id: /**
             * example:
             * 99-9999-9999-99
             */
            Parameters.CourseId;
            migration_id: /**
             * example:
             * 99-9999-9999-99
             */
            Parameters.MigrationId;
            assignment_id: /**
             * example:
             * 99-9999-9999-99
             */
            Parameters.AssignmentId;
        }
        export type RequestBody = /* A grading policy */ Components.Schemas.Policy;
        namespace Responses {
            export type $202 = /* Migration object that has a single assignment and a policy */ Components.Schemas.Migration;
            export type $400 = /* An error occurred while processing that query */ Components.Schemas.ErrorResponse;
            export type $404 = /* An error occurred while processing that query */ Components.Schemas.ErrorResponse;
        }
    }
    namespace UpdateUser {
        export type RequestBody = /* A server user */ Components.Schemas.User;
        namespace Responses {
            export type $202 = /* A server user */ Components.Schemas.User;
        }
    }
    namespace WithdrawExtension {
        namespace Parameters {
            /**
             * example:
             * 999-9999-9999-99
             */
            export type CourseId = string;
            /**
             * example:
             * 999-9999-9999-99
             */
            export type ExtensionId = string;
        }
        export interface PathParameters {
            course_id: /**
             * example:
             * 999-9999-9999-99
             */
            Parameters.CourseId;
            extension_id: /**
             * example:
             * 999-9999-9999-99
             */
            Parameters.ExtensionId;
        }
        namespace Responses {
            export interface $204 {
            }
            export type $404 = /* An error occurred while processing that query */ Components.Schemas.ErrorResponse;
        }
    }
}

export interface OperationMethods {
  /**
   * check_health - Checks the health of the server
   */
  'check_health'(
    parameters?: Parameters<UnknownParamsObject> | null,
    data?: any,
    config?: AxiosRequestConfig  
  ): OperationResponse<Paths.CheckHealth.Responses.$200>
  /**
   * get_courses - Get all courses
   * 
   * Get all courses, returning
   * only active courses by default, and
   * inactive courses if specified. Will
   * return an empty list if there are no
   * courses.
   * 
   */
  'get_courses'(
    parameters?: Parameters<Paths.GetCourses.QueryParameters> | null,
    data?: any,
    config?: AxiosRequestConfig  
  ): OperationResponse<Paths.GetCourses.Responses.$200>
  /**
   * new_course - Create a new course
   * 
   * Create a new course.
   * 
   */
  'new_course'(
    parameters?: Parameters<UnknownParamsObject> | null,
    data?: Paths.NewCourse.RequestBody,
    config?: AxiosRequestConfig  
  ): OperationResponse<Paths.NewCourse.Responses.$201>
  /**
   * get_course - Get existing course
   * 
   * Gets a course
   * 
   */
  'get_course'(
    parameters?: Parameters<Paths.GetCourse.QueryParameters & Paths.GetCourse.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig  
  ): OperationResponse<Paths.GetCourse.Responses.$200>
  /**
   * update_course - Update existing course
   * 
   * Update an existing course.
   * 
   */
  'update_course'(
    parameters?: Parameters<Paths.UpdateCourse.PathParameters> | null,
    data?: Paths.UpdateCourse.RequestBody,
    config?: AxiosRequestConfig  
  ): OperationResponse<Paths.UpdateCourse.Responses.$204>
  /**
   * import_course - Import course from Canvas
   * 
   * Imports data from Canvas into an existing course
   * 
   */
  'import_course'(
    parameters?: Parameters<Paths.ImportCourse.PathParameters> | null,
    data?: Paths.ImportCourse.RequestBody,
    config?: AxiosRequestConfig  
  ): OperationResponse<Paths.ImportCourse.Responses.$202>
  /**
   * sync_course - Sync with established course controller
   * 
   * Syncs with a course in the controller.
   * 
   */
  'sync_course'(
    parameters?: Parameters<Paths.SyncCourse.PathParameters> | null,
    data?: Paths.SyncCourse.RequestBody,
    config?: AxiosRequestConfig  
  ): OperationResponse<Paths.SyncCourse.Responses.$202>
  /**
   * update_assignment - Update an assignment in a course
   * 
   * Update an assignment in a course
   * 
   */
  'update_assignment'(
    parameters?: Parameters<Paths.UpdateAssignment.PathParameters> | null,
    data?: Paths.UpdateAssignment.RequestBody,
    config?: AxiosRequestConfig  
  ): OperationResponse<Paths.UpdateAssignment.Responses.$201>
  /**
   * add_assignment - Add assignment to a course
   * 
   * Add assignments to a course.
   * If any assignment already exists in the class,
   * its information will be updated.
   * 
   */
  'add_assignment'(
    parameters?: Parameters<Paths.AddAssignment.PathParameters> | null,
    data?: Paths.AddAssignment.RequestBody,
    config?: AxiosRequestConfig  
  ): OperationResponse<Paths.AddAssignment.Responses.$201>
  /**
   * disable_assignment - Disable the specified assignment.
   * 
   * Disable the specified assignment.
   * 
   */
  'disable_assignment'(
    parameters?: Parameters<Paths.DisableAssignment.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig  
  ): OperationResponse<Paths.DisableAssignment.Responses.$200>
  /**
   * enable_assignment - Enable the specified assignment.
   * 
   * Enable the specified assignment.
   * 
   */
  'enable_assignment'(
    parameters?: Parameters<Paths.EnableAssignment.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig  
  ): OperationResponse<Paths.EnableAssignment.Responses.$200>
  /**
   * get_members - Get all members for a course
   * 
   * Gets all members for a course.
   * Optional query params can be used to filter by TAs, Instructors, or Students.
   * Can also be used to search by CWID or by name
   * 
   */
  'get_members'(
    parameters?: Parameters<Paths.GetMembers.QueryParameters & Paths.GetMembers.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig  
  ): OperationResponse<Paths.GetMembers.Responses.$200>
  /**
   * update_course_member - Updates a member's membership in a course
   * 
   * Updates a member in a course. If they do not exist, then they are rejected.
   * 
   */
  'update_course_member'(
    parameters?: Parameters<Paths.UpdateCourseMember.PathParameters> | null,
    data?: Paths.UpdateCourseMember.RequestBody,
    config?: AxiosRequestConfig  
  ): OperationResponse<Paths.UpdateCourseMember.Responses.$201>
  /**
   * add_course_member - Add a member to a course
   * 
   * Adds a member to a course. 
   * If they already exist, the request is rejected.
   * If the user does not exist in system, then the request is rejected.
   * 
   */
  'add_course_member'(
    parameters?: Parameters<Paths.AddCourseMember.PathParameters> | null,
    data?: Paths.AddCourseMember.RequestBody,
    config?: AxiosRequestConfig  
  ): OperationResponse<Paths.AddCourseMember.Responses.$201>
  /**
   * admin_get_all_policies - Get all policies
   * 
   * Get all policies for a course
   * 
   */
  'admin_get_all_policies'(
    parameters?: Parameters<Paths.AdminGetAllPolicies.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig  
  ): OperationResponse<Paths.AdminGetAllPolicies.Responses.$200>
  /**
   * new_policy - Create a new policy
   * 
   * Create a new policy for the course
   * 
   */
  'new_policy'(
    parameters?: Parameters<Paths.NewPolicy.PathParameters> | null,
    data?: Paths.NewPolicy.RequestBody,
    config?: AxiosRequestConfig  
  ): OperationResponse<Paths.NewPolicy.Responses.$201>
  /**
   * delete_policy - Create a new policy
   * 
   * Create a new policy for the course
   * 
   */
  'delete_policy'(
    parameters?: Parameters<Paths.DeletePolicy.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig  
  ): OperationResponse<Paths.DeletePolicy.Responses.$204>
  /**
   * get_all_users - Get all users
   * 
   * Gets all the users in the system
   * 
   */
  'get_all_users'(
    parameters?: Parameters<UnknownParamsObject> | null,
    data?: any,
    config?: AxiosRequestConfig  
  ): OperationResponse<Paths.GetAllUsers.Responses.$200>
  /**
   * admin_update_user - Updates a user's information, including admin and disabled/enabled status
   * 
   * Updates a user's information based on the User provided in body and provided JWT.
   * 
   */
  'admin_update_user'(
    parameters?: Parameters<UnknownParamsObject> | null,
    data?: Paths.AdminUpdateUser.RequestBody,
    config?: AxiosRequestConfig  
  ): OperationResponse<Paths.AdminUpdateUser.Responses.$202>
  /**
   * create_user - Create a new user
   * 
   * Creates a new user.
   * 
   */
  'create_user'(
    parameters?: Parameters<UnknownParamsObject> | null,
    data?: Paths.CreateUser.RequestBody,
    config?: AxiosRequestConfig  
  ): OperationResponse<Paths.CreateUser.Responses.$201>
  /**
   * enable_user - enable a user
   * 
   * Enables a user
   * 
   */
  'enable_user'(
    parameters?: Parameters<Paths.EnableUser.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig  
  ): OperationResponse<Paths.EnableUser.Responses.$201>
  /**
   * disable_user - disable a user
   * 
   * Disables a user
   * 
   */
  'disable_user'(
    parameters?: Parameters<Paths.DisableUser.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig  
  ): OperationResponse<Paths.DisableUser.Responses.$201>
  /**
   * get_user - Gets a user's information
   * 
   * Gets a user's information based on the provided JWT.
   * 
   */
  'get_user'(
    parameters?: Parameters<UnknownParamsObject> | null,
    data?: any,
    config?: AxiosRequestConfig  
  ): OperationResponse<Paths.GetUser.Responses.$200>
  /**
   * update_user - Updates a user's information
   * 
   * Updates a user's information based on the User provided in body and provided JWT.
   * 
   */
  'update_user'(
    parameters?: Parameters<UnknownParamsObject> | null,
    data?: Paths.UpdateUser.RequestBody,
    config?: AxiosRequestConfig  
  ): OperationResponse<Paths.UpdateUser.Responses.$202>
  /**
   * get_credentials - Get all credentials for a user
   * 
   * Gets all credentials for the signed in user.
   * A user can only view their own credentials (including admins)
   * 
   */
  'get_credentials'(
    parameters?: Parameters<UnknownParamsObject> | null,
    data?: any,
    config?: AxiosRequestConfig  
  ): OperationResponse<Paths.GetCredentials.Responses.$200>
  /**
   * new_credential - Create a new credential for the user
   * 
   * Create a new credential for a user.
   * 
   * The credential must have a unique name for the user.
   * It must also be the only credential for a service.
   * 
   */
  'new_credential'(
    parameters?: Parameters<UnknownParamsObject> | null,
    data?: Paths.NewCredential.RequestBody,
    config?: AxiosRequestConfig  
  ): OperationResponse<Paths.NewCredential.Responses.$202>
  /**
   * delete_credential - Deletes a credential for the user.
   * 
   * Deletes a selected credential for the user.
   * 
   */
  'delete_credential'(
    parameters?: Parameters<Paths.DeleteCredential.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig  
  ): OperationResponse<Paths.DeleteCredential.Responses.$204>
  /**
   * mark_credential_as_public - Changes the visibility of the credential to public for the user.
   * 
   * Sets a credential's status to public for the user.
   * 
   */
  'mark_credential_as_public'(
    parameters?: Parameters<Paths.MarkCredentialAsPublic.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig  
  ): OperationResponse<Paths.MarkCredentialAsPublic.Responses.$202>
  /**
   * mark_credential_as_private - Changes the visibility of the credential to private for the user.
   * 
   * Sets a credential's status to private for the user.
   * 
   */
  'mark_credential_as_private'(
    parameters?: Parameters<Paths.MarkCredentialAsPrivate.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig  
  ): OperationResponse<Paths.MarkCredentialAsPrivate.Responses.$202>
  /**
   * approve_extension - Approves an extension for an assignment for the user.
   * 
   * Approves an extension for an assignment for the user.
   * 
   */
  'approve_extension'(
    parameters?: Parameters<Paths.ApproveExtension.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig  
  ): OperationResponse<Paths.ApproveExtension.Responses.$202>
  /**
   * deny_extension - Denies an extension for an assignment for the user.
   * 
   * Denies an extension for an assignment for the user.
   * 
   */
  'deny_extension'(
    parameters?: Parameters<Paths.DenyExtension.QueryParameters & Paths.DenyExtension.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig  
  ): OperationResponse<Paths.DenyExtension.Responses.$202>
  /**
   * get_course_information_instructor - Get information for a course
   * 
   * Get information from a course from
   * the instructor's perspective; returns
   * members as an aggregate of all members
   * in their sections.
   * 
   */
  'get_course_information_instructor'(
    parameters?: Parameters<Paths.GetCourseInformationInstructor.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig  
  ): OperationResponse<Paths.GetCourseInformationInstructor.Responses.$200>
  /**
   * get_instructor_enrollments - Get all enrollments for all sections
   * 
   * Gets user enrollments for all sections 
   * an instructor is apart of.
   * 
   */
  'get_instructor_enrollments'(
    parameters?: Parameters<Paths.GetInstructorEnrollments.QueryParameters & Paths.GetInstructorEnrollments.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig  
  ): OperationResponse<Paths.GetInstructorEnrollments.Responses.$200>
  /**
   * get_all_policies - Get all policies
   * 
   * Get all policies for a course
   * 
   */
  'get_all_policies'(
    parameters?: Parameters<Paths.GetAllPolicies.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig  
  ): OperationResponse<Paths.GetAllPolicies.Responses.$200>
  /**
   * get_course_information_student - Get information for a course
   * 
   * Get information from a course from
   * the student's perspective; members and
   * assignments are not returned but the
   * section the student is in is returned.
   * 
   */
  'get_course_information_student'(
    parameters?: Parameters<Paths.GetCourseInformationStudent.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig  
  ): OperationResponse<Paths.GetCourseInformationStudent.Responses.$200>
  /**
   * get_courses_student - Get all courses
   * 
   * Get all active courses for a student
   * 
   */
  'get_courses_student'(
    parameters?: Parameters<UnknownParamsObject> | null,
    data?: any,
    config?: AxiosRequestConfig  
  ): OperationResponse<Paths.GetCoursesStudent.Responses.$200>
  /**
   * get_enrollments - Get all enrollments for a user
   * 
   * Gets all enrollments (classes) that a
   * user is apart of.
   * 
   */
  'get_enrollments'(
    parameters?: Parameters<UnknownParamsObject> | null,
    data?: any,
    config?: AxiosRequestConfig  
  ): OperationResponse<Paths.GetEnrollments.Responses.$200>
  /**
   * get_course_assignments_student - Get assignments for course
   * 
   * Get currently released assignments for course
   * 
   */
  'get_course_assignments_student'(
    parameters?: Parameters<Paths.GetCourseAssignmentsStudent.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig  
  ): OperationResponse<Paths.GetCourseAssignmentsStudent.Responses.$200>
  /**
   * get_all_tasks_for_user - Get all tasks for current user
   * 
   * This endpoint gets all the tasks for the currently signed in user
   * 
   */
  'get_all_tasks_for_user'(
    parameters?: Parameters<UnknownParamsObject> | null,
    data?: any,
    config?: AxiosRequestConfig  
  ): OperationResponse<Paths.GetAllTasksForUser.Responses.$200>
  /**
   * get_task - Get task by id
   * 
   * This endpoint returns the requested task if the user has access to it.
   * 
   */
  'get_task'(
    parameters?: Parameters<Paths.GetTask.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig  
  ): OperationResponse<Paths.GetTask.Responses.$200>
  /**
   * new_extension - Create Extension request
   * 
   * Create a new extension request for an assignment.
   * Replies with an extension request for a user and assignment.
   * 
   */
  'new_extension'(
    parameters?: Parameters<Paths.NewExtension.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig  
  ): OperationResponse<Paths.NewExtension.Responses.$201>
  /**
   * get_all_approved_extensions_for_assignment - Get all the approved extensions for the provided course and assignment
   * 
   * This endpoint gets all approved extensions for a given instructor
   * 
   */
  'get_all_approved_extensions_for_assignment'(
    parameters?: Parameters<Paths.GetAllApprovedExtensionsForAssignment.QueryParameters & Paths.GetAllApprovedExtensionsForAssignment.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig  
  ): OperationResponse<Paths.GetAllApprovedExtensionsForAssignment.Responses.$200>
  /**
   * get_all_approved_extensions_for_member - Get all the approved extensions for the provided member
   * 
   * This endpoint gets all approved extensions for a given member
   * 
   */
  'get_all_approved_extensions_for_member'(
    parameters?: Parameters<Paths.GetAllApprovedExtensionsForMember.QueryParameters & Paths.GetAllApprovedExtensionsForMember.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig  
  ): OperationResponse<Paths.GetAllApprovedExtensionsForMember.Responses.$200>
  /**
   * get_all_extensions_for_section - Get all the extensions for the provided section
   * 
   * This endpoint gets all extensions for a given section
   * 
   */
  'get_all_extensions_for_section'(
    parameters?: Parameters<Paths.GetAllExtensionsForSection.QueryParameters & Paths.GetAllExtensionsForSection.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig  
  ): OperationResponse<Paths.GetAllExtensionsForSection.Responses.$200>
  /**
   * get_all_extensions_for_course - Get all the extensions for the course
   * 
   * This endpoint gets all extensions for a given course
   * 
   */
  'get_all_extensions_for_course'(
    parameters?: Parameters<Paths.GetAllExtensionsForCourse.QueryParameters & Paths.GetAllExtensionsForCourse.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig  
  ): OperationResponse<Paths.GetAllExtensionsForCourse.Responses.$200>
  /**
   * get_all_master_migrations_for_course - Get all the master migrations for the course
   * 
   * This endpoint gets all master migrations for a given course
   * 
   */
  'get_all_master_migrations_for_course'(
    parameters?: Parameters<Paths.GetAllMasterMigrationsForCourse.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig  
  ): OperationResponse<Paths.GetAllMasterMigrationsForCourse.Responses.$200>
  /**
   * create_master_migration - Create master migration for a course
   * 
   * Create a new master migration for a course.
   * Replies with a master migration for a course.
   * 
   */
  'create_master_migration'(
    parameters?: Parameters<Paths.CreateMasterMigration.PathParameters> | null,
    data?: Paths.CreateMasterMigration.RequestBody,
    config?: AxiosRequestConfig  
  ): OperationResponse<Paths.CreateMasterMigration.Responses.$201>
  /**
   * get_all_extensions - Get all extensions for a student
   * 
   * Gets all extensions a student has submitted, regardless
   * of status.
   * 
   */
  'get_all_extensions'(
    parameters?: Parameters<Paths.GetAllExtensions.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig  
  ): OperationResponse<Paths.GetAllExtensions.Responses.$200>
  /**
   * create_extension_request - Student Extension request
   * 
   * Create a new extension request for a student.
   * Replies with an extension request including the request date, assignment impacted, new due date,
   * status, and reason for extension.
   * 
   */
  'create_extension_request'(
    parameters?: Parameters<Paths.CreateExtensionRequest.PathParameters> | null,
    data?: Paths.CreateExtensionRequest.RequestBody,
    config?: AxiosRequestConfig  
  ): OperationResponse<Paths.CreateExtensionRequest.Responses.$201>
  /**
   * withdraw_extension - Withdraw a submitted extension
   * 
   * Deletes (withdraws) a submitted extension
   * by id for the given student.
   * 
   */
  'withdraw_extension'(
    parameters?: Parameters<Paths.WithdrawExtension.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig  
  ): OperationResponse<Paths.WithdrawExtension.Responses.$204>
  /**
   * update_grade - Updates the grade for a migration for the user. Must include a reason for updating the grade.
   * 
   * Updates the grade for a migration for the user.
   * 
   */
  'update_grade'(
    parameters?: Parameters<Paths.UpdateGrade.PathParameters> | null,
    data?: any,
    config?: AxiosRequestConfig  
  ): OperationResponse<Paths.UpdateGrade.Responses.$202>
  /**
   * create_migration_for_master_migration - Create migration for a master migration
   * 
   * Create a new migration for a master migration.
   * Replies with an master migration including the list of migrations, one for each assignment and policy.
   * 
   */
  'create_migration_for_master_migration'(
    parameters?: Parameters<Paths.CreateMigrationForMasterMigration.PathParameters> | null,
    data?: Paths.CreateMigrationForMasterMigration.RequestBody,
    config?: AxiosRequestConfig  
  ): OperationResponse<Paths.CreateMigrationForMasterMigration.Responses.$201>
  /**
   * update_policy - Updates the policy for an assignment and migration
   * 
   * Updates the policy that corresponds to an assignment and a migration.
   * 
   */
  'update_policy'(
    parameters?: Parameters<Paths.UpdatePolicy.PathParameters> | null,
    data?: Paths.UpdatePolicy.RequestBody,
    config?: AxiosRequestConfig  
  ): OperationResponse<Paths.UpdatePolicy.Responses.$202>
}

export interface PathsDictionary {
  ['/-/health']: {
    /**
     * check_health - Checks the health of the server
     */
    'get'(
      parameters?: Parameters<UnknownParamsObject> | null,
      data?: any,
      config?: AxiosRequestConfig  
    ): OperationResponse<Paths.CheckHealth.Responses.$200>
  }
  ['/admin/courses']: {
    /**
     * get_courses - Get all courses
     * 
     * Get all courses, returning
     * only active courses by default, and
     * inactive courses if specified. Will
     * return an empty list if there are no
     * courses.
     * 
     */
    'get'(
      parameters?: Parameters<Paths.GetCourses.QueryParameters> | null,
      data?: any,
      config?: AxiosRequestConfig  
    ): OperationResponse<Paths.GetCourses.Responses.$200>
    /**
     * new_course - Create a new course
     * 
     * Create a new course.
     * 
     */
    'post'(
      parameters?: Parameters<UnknownParamsObject> | null,
      data?: Paths.NewCourse.RequestBody,
      config?: AxiosRequestConfig  
    ): OperationResponse<Paths.NewCourse.Responses.$201>
  }
  ['/admin/courses/{course_id}']: {
    /**
     * update_course - Update existing course
     * 
     * Update an existing course.
     * 
     */
    'put'(
      parameters?: Parameters<Paths.UpdateCourse.PathParameters> | null,
      data?: Paths.UpdateCourse.RequestBody,
      config?: AxiosRequestConfig  
    ): OperationResponse<Paths.UpdateCourse.Responses.$204>
    /**
     * get_course - Get existing course
     * 
     * Gets a course
     * 
     */
    'get'(
      parameters?: Parameters<Paths.GetCourse.QueryParameters & Paths.GetCourse.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig  
    ): OperationResponse<Paths.GetCourse.Responses.$200>
  }
  ['/admin/courses/{course_id}/import']: {
    /**
     * import_course - Import course from Canvas
     * 
     * Imports data from Canvas into an existing course
     * 
     */
    'post'(
      parameters?: Parameters<Paths.ImportCourse.PathParameters> | null,
      data?: Paths.ImportCourse.RequestBody,
      config?: AxiosRequestConfig  
    ): OperationResponse<Paths.ImportCourse.Responses.$202>
  }
  ['/admin/courses/{course_id}/sync']: {
    /**
     * sync_course - Sync with established course controller
     * 
     * Syncs with a course in the controller.
     * 
     */
    'post'(
      parameters?: Parameters<Paths.SyncCourse.PathParameters> | null,
      data?: Paths.SyncCourse.RequestBody,
      config?: AxiosRequestConfig  
    ): OperationResponse<Paths.SyncCourse.Responses.$202>
  }
  ['/admin/courses/{course_id}/assignments']: {
    /**
     * update_assignment - Update an assignment in a course
     * 
     * Update an assignment in a course
     * 
     */
    'put'(
      parameters?: Parameters<Paths.UpdateAssignment.PathParameters> | null,
      data?: Paths.UpdateAssignment.RequestBody,
      config?: AxiosRequestConfig  
    ): OperationResponse<Paths.UpdateAssignment.Responses.$201>
    /**
     * add_assignment - Add assignment to a course
     * 
     * Add assignments to a course.
     * If any assignment already exists in the class,
     * its information will be updated.
     * 
     */
    'post'(
      parameters?: Parameters<Paths.AddAssignment.PathParameters> | null,
      data?: Paths.AddAssignment.RequestBody,
      config?: AxiosRequestConfig  
    ): OperationResponse<Paths.AddAssignment.Responses.$201>
  }
  ['/admin/courses/{course_id}/assignment/{assignment_id}/disable']: {
    /**
     * disable_assignment - Disable the specified assignment.
     * 
     * Disable the specified assignment.
     * 
     */
    'put'(
      parameters?: Parameters<Paths.DisableAssignment.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig  
    ): OperationResponse<Paths.DisableAssignment.Responses.$200>
  }
  ['/admin/courses/{course_id}/assignment/{assignment_id}/enable']: {
    /**
     * enable_assignment - Enable the specified assignment.
     * 
     * Enable the specified assignment.
     * 
     */
    'put'(
      parameters?: Parameters<Paths.EnableAssignment.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig  
    ): OperationResponse<Paths.EnableAssignment.Responses.$200>
  }
  ['/admin/courses/{course_id}/members']: {
    /**
     * get_members - Get all members for a course
     * 
     * Gets all members for a course.
     * Optional query params can be used to filter by TAs, Instructors, or Students.
     * Can also be used to search by CWID or by name
     * 
     */
    'get'(
      parameters?: Parameters<Paths.GetMembers.QueryParameters & Paths.GetMembers.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig  
    ): OperationResponse<Paths.GetMembers.Responses.$200>
    /**
     * add_course_member - Add a member to a course
     * 
     * Adds a member to a course. 
     * If they already exist, the request is rejected.
     * If the user does not exist in system, then the request is rejected.
     * 
     */
    'post'(
      parameters?: Parameters<Paths.AddCourseMember.PathParameters> | null,
      data?: Paths.AddCourseMember.RequestBody,
      config?: AxiosRequestConfig  
    ): OperationResponse<Paths.AddCourseMember.Responses.$201>
    /**
     * update_course_member - Updates a member's membership in a course
     * 
     * Updates a member in a course. If they do not exist, then they are rejected.
     * 
     */
    'put'(
      parameters?: Parameters<Paths.UpdateCourseMember.PathParameters> | null,
      data?: Paths.UpdateCourseMember.RequestBody,
      config?: AxiosRequestConfig  
    ): OperationResponse<Paths.UpdateCourseMember.Responses.$201>
  }
  ['/admin/courses/{course_id}/policies']: {
    /**
     * new_policy - Create a new policy
     * 
     * Create a new policy for the course
     * 
     */
    'post'(
      parameters?: Parameters<Paths.NewPolicy.PathParameters> | null,
      data?: Paths.NewPolicy.RequestBody,
      config?: AxiosRequestConfig  
    ): OperationResponse<Paths.NewPolicy.Responses.$201>
    /**
     * admin_get_all_policies - Get all policies
     * 
     * Get all policies for a course
     * 
     */
    'get'(
      parameters?: Parameters<Paths.AdminGetAllPolicies.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig  
    ): OperationResponse<Paths.AdminGetAllPolicies.Responses.$200>
  }
  ['/admin/course/{course_id}/policies/{policy_id}']: {
    /**
     * delete_policy - Create a new policy
     * 
     * Create a new policy for the course
     * 
     */
    'delete'(
      parameters?: Parameters<Paths.DeletePolicy.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig  
    ): OperationResponse<Paths.DeletePolicy.Responses.$204>
  }
  ['/admin/users']: {
    /**
     * get_all_users - Get all users
     * 
     * Gets all the users in the system
     * 
     */
    'get'(
      parameters?: Parameters<UnknownParamsObject> | null,
      data?: any,
      config?: AxiosRequestConfig  
    ): OperationResponse<Paths.GetAllUsers.Responses.$200>
    /**
     * create_user - Create a new user
     * 
     * Creates a new user.
     * 
     */
    'post'(
      parameters?: Parameters<UnknownParamsObject> | null,
      data?: Paths.CreateUser.RequestBody,
      config?: AxiosRequestConfig  
    ): OperationResponse<Paths.CreateUser.Responses.$201>
    /**
     * admin_update_user - Updates a user's information, including admin and disabled/enabled status
     * 
     * Updates a user's information based on the User provided in body and provided JWT.
     * 
     */
    'put'(
      parameters?: Parameters<UnknownParamsObject> | null,
      data?: Paths.AdminUpdateUser.RequestBody,
      config?: AxiosRequestConfig  
    ): OperationResponse<Paths.AdminUpdateUser.Responses.$202>
  }
  ['/admin/users/{cwid}/enable']: {
    /**
     * enable_user - enable a user
     * 
     * Enables a user
     * 
     */
    'put'(
      parameters?: Parameters<Paths.EnableUser.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig  
    ): OperationResponse<Paths.EnableUser.Responses.$201>
  }
  ['/admin/users/{cwid}/disable']: {
    /**
     * disable_user - disable a user
     * 
     * Disables a user
     * 
     */
    'put'(
      parameters?: Parameters<Paths.DisableUser.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig  
    ): OperationResponse<Paths.DisableUser.Responses.$201>
  }
  ['/user']: {
    /**
     * get_user - Gets a user's information
     * 
     * Gets a user's information based on the provided JWT.
     * 
     */
    'get'(
      parameters?: Parameters<UnknownParamsObject> | null,
      data?: any,
      config?: AxiosRequestConfig  
    ): OperationResponse<Paths.GetUser.Responses.$200>
    /**
     * update_user - Updates a user's information
     * 
     * Updates a user's information based on the User provided in body and provided JWT.
     * 
     */
    'put'(
      parameters?: Parameters<UnknownParamsObject> | null,
      data?: Paths.UpdateUser.RequestBody,
      config?: AxiosRequestConfig  
    ): OperationResponse<Paths.UpdateUser.Responses.$202>
  }
  ['/user/credentials']: {
    /**
     * new_credential - Create a new credential for the user
     * 
     * Create a new credential for a user.
     * 
     * The credential must have a unique name for the user.
     * It must also be the only credential for a service.
     * 
     */
    'post'(
      parameters?: Parameters<UnknownParamsObject> | null,
      data?: Paths.NewCredential.RequestBody,
      config?: AxiosRequestConfig  
    ): OperationResponse<Paths.NewCredential.Responses.$202>
    /**
     * get_credentials - Get all credentials for a user
     * 
     * Gets all credentials for the signed in user.
     * A user can only view their own credentials (including admins)
     * 
     */
    'get'(
      parameters?: Parameters<UnknownParamsObject> | null,
      data?: any,
      config?: AxiosRequestConfig  
    ): OperationResponse<Paths.GetCredentials.Responses.$200>
  }
  ['/user/credentials/{credential_id}/delete']: {
    /**
     * delete_credential - Deletes a credential for the user.
     * 
     * Deletes a selected credential for the user.
     * 
     */
    'delete'(
      parameters?: Parameters<Paths.DeleteCredential.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig  
    ): OperationResponse<Paths.DeleteCredential.Responses.$204>
  }
  ['/user/credentials/{credential_id}/public']: {
    /**
     * mark_credential_as_public - Changes the visibility of the credential to public for the user.
     * 
     * Sets a credential's status to public for the user.
     * 
     */
    'put'(
      parameters?: Parameters<Paths.MarkCredentialAsPublic.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig  
    ): OperationResponse<Paths.MarkCredentialAsPublic.Responses.$202>
  }
  ['/user/credentials/{credential_id}/private']: {
    /**
     * mark_credential_as_private - Changes the visibility of the credential to private for the user.
     * 
     * Sets a credential's status to private for the user.
     * 
     */
    'put'(
      parameters?: Parameters<Paths.MarkCredentialAsPrivate.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig  
    ): OperationResponse<Paths.MarkCredentialAsPrivate.Responses.$202>
  }
  ['/instructor/courses/{course_id}/assignment/{assignment_id}/user/{user_id}/extensions/{extension_id}/approve']: {
    /**
     * approve_extension - Approves an extension for an assignment for the user.
     * 
     * Approves an extension for an assignment for the user.
     * 
     */
    'put'(
      parameters?: Parameters<Paths.ApproveExtension.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig  
    ): OperationResponse<Paths.ApproveExtension.Responses.$202>
  }
  ['/instructor/courses/{course_id}/assignment/{assignment_id}/user/{user_id}/extensions/{extension_id}/deny']: {
    /**
     * deny_extension - Denies an extension for an assignment for the user.
     * 
     * Denies an extension for an assignment for the user.
     * 
     */
    'put'(
      parameters?: Parameters<Paths.DenyExtension.QueryParameters & Paths.DenyExtension.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig  
    ): OperationResponse<Paths.DenyExtension.Responses.$202>
  }
  ['/instructor/courses/{course_id}']: {
    /**
     * get_course_information_instructor - Get information for a course
     * 
     * Get information from a course from
     * the instructor's perspective; returns
     * members as an aggregate of all members
     * in their sections.
     * 
     */
    'get'(
      parameters?: Parameters<Paths.GetCourseInformationInstructor.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig  
    ): OperationResponse<Paths.GetCourseInformationInstructor.Responses.$200>
  }
  ['/instructor/courses/{course_id}/enrollments']: {
    /**
     * get_instructor_enrollments - Get all enrollments for all sections
     * 
     * Gets user enrollments for all sections 
     * an instructor is apart of.
     * 
     */
    'get'(
      parameters?: Parameters<Paths.GetInstructorEnrollments.QueryParameters & Paths.GetInstructorEnrollments.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig  
    ): OperationResponse<Paths.GetInstructorEnrollments.Responses.$200>
  }
  ['/instructor/courses/{course_id}/policies']: {
    /**
     * get_all_policies - Get all policies
     * 
     * Get all policies for a course
     * 
     */
    'get'(
      parameters?: Parameters<Paths.GetAllPolicies.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig  
    ): OperationResponse<Paths.GetAllPolicies.Responses.$200>
  }
  ['/student/courses/{course_id}']: {
    /**
     * get_course_information_student - Get information for a course
     * 
     * Get information from a course from
     * the student's perspective; members and
     * assignments are not returned but the
     * section the student is in is returned.
     * 
     */
    'get'(
      parameters?: Parameters<Paths.GetCourseInformationStudent.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig  
    ): OperationResponse<Paths.GetCourseInformationStudent.Responses.$200>
  }
  ['/student/courses']: {
    /**
     * get_courses_student - Get all courses
     * 
     * Get all active courses for a student
     * 
     */
    'get'(
      parameters?: Parameters<UnknownParamsObject> | null,
      data?: any,
      config?: AxiosRequestConfig  
    ): OperationResponse<Paths.GetCoursesStudent.Responses.$200>
  }
  ['/user/enrollments']: {
    /**
     * get_enrollments - Get all enrollments for a user
     * 
     * Gets all enrollments (classes) that a
     * user is apart of.
     * 
     */
    'get'(
      parameters?: Parameters<UnknownParamsObject> | null,
      data?: any,
      config?: AxiosRequestConfig  
    ): OperationResponse<Paths.GetEnrollments.Responses.$200>
  }
  ['/student/courses/{course_id}/assignments']: {
    /**
     * get_course_assignments_student - Get assignments for course
     * 
     * Get currently released assignments for course
     * 
     */
    'get'(
      parameters?: Parameters<Paths.GetCourseAssignmentsStudent.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig  
    ): OperationResponse<Paths.GetCourseAssignmentsStudent.Responses.$200>
  }
  ['/tasks']: {
    /**
     * get_all_tasks_for_user - Get all tasks for current user
     * 
     * This endpoint gets all the tasks for the currently signed in user
     * 
     */
    'get'(
      parameters?: Parameters<UnknownParamsObject> | null,
      data?: any,
      config?: AxiosRequestConfig  
    ): OperationResponse<Paths.GetAllTasksForUser.Responses.$200>
  }
  ['/tasks/{task_id}']: {
    /**
     * get_task - Get task by id
     * 
     * This endpoint returns the requested task if the user has access to it.
     * 
     */
    'get'(
      parameters?: Parameters<Paths.GetTask.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig  
    ): OperationResponse<Paths.GetTask.Responses.$200>
  }
  ['/instructor/courses/{course_id}/assignments/{assignment_id}/user/{user_id}/extensions']: {
    /**
     * new_extension - Create Extension request
     * 
     * Create a new extension request for an assignment.
     * Replies with an extension request for a user and assignment.
     * 
     */
    'post'(
      parameters?: Parameters<Paths.NewExtension.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig  
    ): OperationResponse<Paths.NewExtension.Responses.$201>
  }
  ['/instructor/courses/{course_id}/assignments/{assignment_id}/extensions/{extension_id}']: {
    /**
     * get_all_approved_extensions_for_assignment - Get all the approved extensions for the provided course and assignment
     * 
     * This endpoint gets all approved extensions for a given instructor
     * 
     */
    'get'(
      parameters?: Parameters<Paths.GetAllApprovedExtensionsForAssignment.QueryParameters & Paths.GetAllApprovedExtensionsForAssignment.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig  
    ): OperationResponse<Paths.GetAllApprovedExtensionsForAssignment.Responses.$200>
  }
  ['/instructor/courses/{course_id}/users/{user_id}/extensions/{extension_id}']: {
    /**
     * get_all_approved_extensions_for_member - Get all the approved extensions for the provided member
     * 
     * This endpoint gets all approved extensions for a given member
     * 
     */
    'get'(
      parameters?: Parameters<Paths.GetAllApprovedExtensionsForMember.QueryParameters & Paths.GetAllApprovedExtensionsForMember.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig  
    ): OperationResponse<Paths.GetAllApprovedExtensionsForMember.Responses.$200>
  }
  ['/instructor/courses/{course_id}/sections/{section_id}/extensions/{extension_id}']: {
    /**
     * get_all_extensions_for_section - Get all the extensions for the provided section
     * 
     * This endpoint gets all extensions for a given section
     * 
     */
    'get'(
      parameters?: Parameters<Paths.GetAllExtensionsForSection.QueryParameters & Paths.GetAllExtensionsForSection.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig  
    ): OperationResponse<Paths.GetAllExtensionsForSection.Responses.$200>
  }
  ['/admin/courses/{course_id}/extensions']: {
    /**
     * get_all_extensions_for_course - Get all the extensions for the course
     * 
     * This endpoint gets all extensions for a given course
     * 
     */
    'get'(
      parameters?: Parameters<Paths.GetAllExtensionsForCourse.QueryParameters & Paths.GetAllExtensionsForCourse.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig  
    ): OperationResponse<Paths.GetAllExtensionsForCourse.Responses.$200>
  }
  ['/instructor/courses/{course_id}/migrations']: {
    /**
     * get_all_master_migrations_for_course - Get all the master migrations for the course
     * 
     * This endpoint gets all master migrations for a given course
     * 
     */
    'get'(
      parameters?: Parameters<Paths.GetAllMasterMigrationsForCourse.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig  
    ): OperationResponse<Paths.GetAllMasterMigrationsForCourse.Responses.$200>
    /**
     * create_master_migration - Create master migration for a course
     * 
     * Create a new master migration for a course.
     * Replies with a master migration for a course.
     * 
     */
    'post'(
      parameters?: Parameters<Paths.CreateMasterMigration.PathParameters> | null,
      data?: Paths.CreateMasterMigration.RequestBody,
      config?: AxiosRequestConfig  
    ): OperationResponse<Paths.CreateMasterMigration.Responses.$201>
  }
  ['/student/courses/{course_id}/extensions']: {
    /**
     * get_all_extensions - Get all extensions for a student
     * 
     * Gets all extensions a student has submitted, regardless
     * of status.
     * 
     */
    'get'(
      parameters?: Parameters<Paths.GetAllExtensions.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig  
    ): OperationResponse<Paths.GetAllExtensions.Responses.$200>
    /**
     * create_extension_request - Student Extension request
     * 
     * Create a new extension request for a student.
     * Replies with an extension request including the request date, assignment impacted, new due date,
     * status, and reason for extension.
     * 
     */
    'post'(
      parameters?: Parameters<Paths.CreateExtensionRequest.PathParameters> | null,
      data?: Paths.CreateExtensionRequest.RequestBody,
      config?: AxiosRequestConfig  
    ): OperationResponse<Paths.CreateExtensionRequest.Responses.$201>
  }
  ['/student/courses/{course_id}/extensions/{extension_id}/withdraw']: {
    /**
     * withdraw_extension - Withdraw a submitted extension
     * 
     * Deletes (withdraws) a submitted extension
     * by id for the given student.
     * 
     */
    'delete'(
      parameters?: Parameters<Paths.WithdrawExtension.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig  
    ): OperationResponse<Paths.WithdrawExtension.Responses.$204>
  }
  ['/instructor/courses/{course_id}/migrations/{master_migration_id}']: {
    /**
     * update_grade - Updates the grade for a migration for the user. Must include a reason for updating the grade.
     * 
     * Updates the grade for a migration for the user.
     * 
     */
    'put'(
      parameters?: Parameters<Paths.UpdateGrade.PathParameters> | null,
      data?: any,
      config?: AxiosRequestConfig  
    ): OperationResponse<Paths.UpdateGrade.Responses.$202>
    /**
     * create_migration_for_master_migration - Create migration for a master migration
     * 
     * Create a new migration for a master migration.
     * Replies with an master migration including the list of migrations, one for each assignment and policy.
     * 
     */
    'post'(
      parameters?: Parameters<Paths.CreateMigrationForMasterMigration.PathParameters> | null,
      data?: Paths.CreateMigrationForMasterMigration.RequestBody,
      config?: AxiosRequestConfig  
    ): OperationResponse<Paths.CreateMigrationForMasterMigration.Responses.$201>
  }
  ['/instructor/courses/{course_id}/migrations/{migration_id}/assignments/{assignment_id}']: {
    /**
     * update_policy - Updates the policy for an assignment and migration
     * 
     * Updates the policy that corresponds to an assignment and a migration.
     * 
     */
    'put'(
      parameters?: Parameters<Paths.UpdatePolicy.PathParameters> | null,
      data?: Paths.UpdatePolicy.RequestBody,
      config?: AxiosRequestConfig  
    ): OperationResponse<Paths.UpdatePolicy.Responses.$202>
  }
}

export type Client = OpenAPIClient<OperationMethods, PathsDictionary>

export type Assignment = Components.Schemas.Assignment;
export type AssignmentSlim = Components.Schemas.AssignmentSlim;
export type Course = Components.Schemas.Course;
export type CourseLateRequestConfig = Components.Schemas.CourseLateRequestConfig;
export type CourseMember = Components.Schemas.CourseMember;
export type CourseSlim = Components.Schemas.CourseSlim;
export type CourseSyncTask = Components.Schemas.CourseSyncTask;
export type Credential = Components.Schemas.Credential;
export type Enrollment = Components.Schemas.Enrollment;
export type ErrorResponse = Components.Schemas.ErrorResponse;
export type Extension = Components.Schemas.Extension;
export type LateRequest = Components.Schemas.LateRequest;
export type MasterMigration = Components.Schemas.MasterMigration;
export type MasterMigrationStatistics = Components.Schemas.MasterMigrationStatistics;
export type Migration = Components.Schemas.Migration;
export type NewPolicy = Components.Schemas.NewPolicy;
export type Policy = Components.Schemas.Policy;
export type StudentInformation = Components.Schemas.StudentInformation;
export type Task = Components.Schemas.Task;
export type User = Components.Schemas.User;
