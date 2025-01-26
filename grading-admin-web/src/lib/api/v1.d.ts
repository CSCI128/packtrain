/**
 * This file was auto-generated by openapi-typescript.
 * Do not make direct changes to the file.
 */

export interface paths {
    "/admin/course/new/{canvas_id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * Import course from Canvas
         * @description Create a new course in system using a Canvas course as a template.
         *     Replies with populated template values, but those values must be committed by updating the course.
         *
         */
        put: operations["new_course"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/admin/course/{course_id}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Get existing course
         * @description Gets a course
         *
         */
        get: operations["get_course"];
        /**
         * Update existing course
         * @description Update an existing course by replacing all content in the course.
         *     If any assignments have been graded, then this will fail.
         *
         */
        put: operations["update_course"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/admin/course/{course_id}/update": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * Sync with established course controller
         * @description Syncs with a course in the controller.
         *
         */
        put: operations["sync_course"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/admin/course/{course_id}/students": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Get all students for a course
         * @description Get all information about the students in a course.
         *     This is different from the other ones, as it returns all the metadata about a student.
         *
         */
        get: operations["get_students"];
        /**
         * Add students to a course
         * @description Add students to a course.
         *     If any student already exist in the class (based off of the students CWID),
         *     their information will be updated.
         *
         */
        put: operations["add_students"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/admin/course/{course_id}/assignments": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Get all assignments for a course
         * @description Get all information about the assignments in a course.
         *     This is different from the other ones, as it returns all the metadata about an assignments.
         *
         */
        get: operations["get_assignments"];
        /**
         * Add assignment to a course
         * @description Add assignments to a course.
         *     If any assignment already exist in the class (based off of the TODO),
         *     their information will be updated.
         *
         */
        put: operations["add_assignment"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/admin/course/{course_id}/enable": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * Enable the specified course.
         * @description Enable the specified course.
         *
         */
        put: operations["enable_course"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/admin/course/{course_id}/disable": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * Disable the specified course.
         * @description Disable the specified course.
         *
         */
        put: operations["disable_course"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/admin/course/{course_id}/instructor": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * Add an instructor to a course
         * @description Add an instructor to a course.
         *     If the instructor already exists in the class (based off of CWID),
         *     their information will be updated.
         *
         */
        put: operations["add_instructor"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
}
export type webhooks = Record<string, never>;
export interface components {
    schemas: {
        /** @description An error occurred while processing that query */
        ErrorResponse: {
            error_source?: string;
            error_message?: string;
        };
        /** @description A user in a course */
        CourseMember: {
            /** @example 99999999 */
            cwid: string;
            /** @example self */
            canvas_id: string;
            /**
             * @example owner
             * @enum {string}
             */
            course_role: "student" | "teacher" | "ta" | "owner";
            /** @example fall.2020.excl.101.section.a */
            section?: string;
        };
        /** @description An assignment in a course */
        Assignment: {
            /**
             * Format: double
             * @example 15
             */
            points: number;
            /**
             * Format: date-time
             * @example 2020-01-15T12:00:00.000Z
             */
            due_date: string;
            /**
             * Format: date-time
             * @example 2020-01-01T12:00:00.000Z
             */
            unlock_date: string;
            /** @example true */
            enabled: boolean;
            /** @example Quiz */
            category: string;
            /** @example 12345678 */
            canvas_id: string;
        };
        /** @description A complete course */
        Course: {
            /** @example 999-9999-9999-99 */
            id?: string;
            /** @example Fall 2020 */
            term: string;
            /** @example true */
            enabled: boolean;
            /** @example EXCL101 */
            name: string;
            /** @example Fall.2020.EXCL.101 */
            code: string;
            /** @example 123456 */
            canvas_id: string;
            members?: components["schemas"]["CourseMember"][];
            assignments?: components["schemas"]["Assignment"][];
            /** @example [
             *       "fall.2020.excl.101.section.a"
             *     ] */
            sections?: string[];
        };
    };
    responses: never;
    parameters: never;
    requestBodies: never;
    headers: never;
    pathItems: never;
}
export type $defs = Record<string, never>;
export interface operations {
    new_course: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description The Canvas course ID */
                canvas_id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description A empty course has been created */
            201: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Course"];
                };
            };
            /** @description An authentication error occurred */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ErrorResponse"];
                };
            };
            /** @description Canvas course not found */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ErrorResponse"];
                };
            };
        };
    };
    get_course: {
        parameters: {
            query?: {
                include?: ("members" | "assignments" | "sections")[];
            };
            header?: never;
            path: {
                course_id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Course"];
                };
            };
        };
    };
    update_course: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                course_id: string;
            };
            cookie?: never;
        };
        requestBody?: {
            content: {
                "application/json": components["schemas"]["Course"];
            };
        };
        responses: {
            /** @description Course update request accepted */
            202: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
            /** @description Refused to update course */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ErrorResponse"];
                };
            };
            /** @description Course does not exist */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ErrorResponse"];
                };
            };
        };
    };
    sync_course: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                course_id: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": unknown;
            };
        };
        responses: {
            /** @description Accepted */
            201: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
            /** @description Course Not Found */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ErrorResponse"];
                };
            };
        };
    };
    get_students: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                course_id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["CourseMember"][];
                };
            };
            /** @description Course Not Found */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ErrorResponse"];
                };
            };
        };
    };
    add_students: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                course_id: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CourseMember"][];
            };
        };
        responses: {
            /** @description Accepted */
            201: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
            /** @description Course Not Found */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ErrorResponse"];
                };
            };
        };
    };
    get_assignments: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                course_id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["Assignment"][];
                };
            };
            /** @description Course Not Found */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ErrorResponse"];
                };
            };
        };
    };
    add_assignment: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                course_id: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["Assignment"][];
            };
        };
        responses: {
            /** @description Accepted */
            201: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
            /** @description Course Not Found */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ErrorResponse"];
                };
            };
        };
    };
    enable_course: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                course_id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
            /** @description Course Not Found */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ErrorResponse"];
                };
            };
        };
    };
    disable_course: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                course_id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description OK */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
            /** @description Course Not Found */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ErrorResponse"];
                };
            };
        };
    };
    add_instructor: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                course_id: string;
            };
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["CourseMember"];
            };
        };
        responses: {
            /** @description Accepted */
            201: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
            /** @description Course Not Found */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ErrorResponse"];
                };
            };
        };
    };
}
