import { Column } from "./column.model";
import { Members } from "./members.model";

export class Board {
    id?: string;
    userId?: string;
    name: string = '';
    columns: Column[] = [];
    added_columns: Column[] = [];
    selectedTask: string = '';
    globalOption: string = '';
    members: Members[] = [];

    /**
     * @param id - board id
     * @param userId - owning user id
     * @param name - board name
     * @param columns - a single column or an array of columns, normalized to an array
     * @param selectedTask - id of the currently selected task, if any
     * @param globalOption - UI state for the currently open global option/menu
     * @param members - a single member or an array of members, normalized to an array
     */
    constructor(id?: string, userId?: string, name: string = '', columns?: Column | Column[],
       selectedTask: string = '', globalOption: string = '', members?: Members | Members[]) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        if (columns) {
            this.columns = Array.isArray(columns) ? columns : [columns];
        }
        this.selectedTask = selectedTask;
        this.globalOption = globalOption;
        if (members) {
            this.members = Array.isArray(members) ? members : [members];
        }
    }


}
