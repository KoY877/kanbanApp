export class User {
    id?: string;
    username: string = '';
    email: string = '';
    role?: string;

    /**
     * @param id - user id
     * @param username - display name
     * @param email - user email
     * @param role - user role (e.g. USER, ADMINISTRATOR)
     */
    constructor(id?: string, username: string = '', email: string = '', role?: string) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
    }
}
