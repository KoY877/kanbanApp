import { ChangeDetectorRef, Component, ElementRef, EventEmitter, HostListener, Input, Output } from '@angular/core';
import { FormArray, FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { lastValueFrom, Subject} from 'rxjs';
import { Board as BoardModel } from '../../../models/board.model';
import { EntityService } from '../../../services/entity-service';
import { Reload } from '../../../services/reload';
import { CommonModule } from '@angular/common';
import { CharacterPipe } from '../../../pipes/character-pipe';
import { faArrowLeft, faQuestion } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { Members } from '../../../models/members.model';
import { MemberService } from '../../../services/member-service';

@Component({
  selector: 'app-invite-member',
  imports: [CommonModule, ReactiveFormsModule, CharacterPipe, FontAwesomeModule],
  templateUrl: './invite-member.html',
  styleUrl: './invite-member.css',
})
export class InviteMember {
  // Fontawesome icon
  readonly faQuestion = faQuestion;
  readonly faArrowLeft = faArrowLeft;

  // Input and Output for send data and event on the board component
  @Input() getBoardId?: any
  @Output() closeInviteModal: EventEmitter<void> = new EventEmitter<void>();
  @Output() clickOutside = new EventEmitter<any>();
  @Output() clickOutside2 = new EventEmitter<any>();
  @Output() isCompleteEmailChange = new EventEmitter<any>();
  private destroy$ = new Subject<void>();

  boardId = '';
  ownerName = '';
  ownerInitials = '';
  currentStep:number = 1;
  color?: string | null = '#'
  selectedIndex: number | null = null;
  selectedRole: string = 'Standard';
  data?: Array<any>[]
  alertMessage: string = '';
  receiveMembersData?: Array<any>[]
  notDuplicateMemberData?:  Array<{ memberEmail: string; role: string }> = [];
  boardData?: any
  isCompleteEmail: boolean = false
  isShowEmail: boolean = false
  isValueChange: boolean = false
  dropdownValue: boolean = false
  emailValue: string = ""
  form: FormGroup;
  memberColors: string[] = [];
  newMembers: Members[] = [];

  constructor (
    private formBuilder: FormBuilder,
    private entityService : EntityService,
    private memberService: MemberService,
    private detectChange : ChangeDetectorRef,
    private elementRef: ElementRef,
    private reloadPage: Reload
  ) {
    this.form = this.formBuilder.group({
      email: ['', Validators.email],
      members: formBuilder.array([]),
      chooseRole: ['Standard']
    })
  }

  /**
   * Lifecycle hook: validate the email input as the user types and assign a
   * random color to each already-present member row.
   */
  ngOnInit(): void {
    this.email.valueChanges.subscribe((value: string) => {
      if (value && value.trim() !== '') {
        this.isCompleteEmail = true;
        const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
        this.isShowEmail = emailRegex.test(value);
        this.emailValue = value;
      } else {
        this.isCompleteEmail = false;
        this.isShowEmail = false;
        this.emailValue = '';
      }
    });

    this.memberColors = this.members.controls.map(() => this.getRandomColor());
  }

  /** Lifecycle hook: complete the destroy$ subject to unsubscribe all pipes. */
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * When a click occurs outside this component, either reset the email
   * input (nothing pending) or notify the parent that an email is pending.
   * @param event - the document click event
   */
  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.elementRef.nativeElement.contains(event.target)) {

      // If  email value in Invite members change
      if (this.isValueChange === false && this.isCompleteEmail === false) {
        this.clickOutside.emit(this.isValueChange);
        this.email.reset()
        this.dropdownValue = false
      } else  {

        this.isCompleteEmailChange.emit(true);
      }
    }
  }

  /** Close the invite-member dropdown. */
  handleCloseDropdown() {
    this.closeInviteModal.emit()
  }

  /** The single-email input control. */
  get email(): FormControl {
    return this.form.get('email') as FormControl;
  }

  /** The FormArray holding the members staged for invitation. */
  get members(): FormArray {
    return this.form.get('members') as FormArray;
  }

  /** The role selected for the members being invited. */
  get chooseRole(): FormControl {
    return this.form.get('chooseRole') as FormControl;
  }

  /**
   * Validate the current email input and add it to the staged members list,
   * unless it is invalid, already on the board, or already staged.
   * @param event - the form submit/click event
   * @param id - the board id
   */
  async addEmail(event: any, id: string) {
    event.preventDefault();

    const emailValue: string = this.email.value?.trim();
    const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;

    if (emailValue && emailRegex.test(emailValue)) {
      // Get current board data
      const currentBoard = await lastValueFrom(
        this.entityService.getDataById<BoardModel>('boards', id)
      );

      if (!currentBoard || !currentBoard[0]) {
        alert('Board not found');
        return;
      }

      // Check if email already exists in board
      const emailExists = currentBoard[0].members?.some(
        (m: Members) => m.memberEmail?.toLowerCase() === emailValue.toLowerCase()
      );

      if (emailExists) {
        alert('This member is already part of the board');
        this.email.reset();
        return;
      }

      // Check if email already in the form
      const alreadyAdded = this.members.controls.some(
        (control) => control.get('memberEmail')?.value.toLowerCase() === emailValue.toLowerCase()
      );

      if (alreadyAdded) {
        alert('This email is already in the list');
        this.email.reset();
        return;
      }

      // Add email to members array
      this.members.push(this.formBuilder.group({
        memberEmail: [emailValue],
        role: ['Standard'],
        boardId: [this.boardId]
      })),
      this.email.reset()

      // Reset email input
      this.email.reset();
      this.isCompleteEmail = false;
      this.isShowEmail = false;
    } else {
      alert('Please enter a valid email address');
    }
  }

  /**
   * Remove a staged email from the members list.
   * @param event - the click event
   * @param index - index of the entry to remove in the members FormArray
   */
  removeEmail(event: any, index: number) {
    event.stopPropagation();
    event.preventDefault();
    this.members.removeAt(index);
  }

  /** Advance the wizard to the role-selection step (step 2). */
  moveToNextStep (){
    this.currentStep = 2
    this.detectChange.detectChanges()
  }

  /**
   * Add any pending email input, deduplicate staged members, and advance
   * to the role-selection step.
   * @param event - the form submit event
   * @param id - the board id
   */
  async nextStep(event: any, id: string) {
    event.preventDefault();

    try {
      const emailValue: string | null = this.email.value?.trim();

      // Regex to valid email address
      const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;

      // If there's an email in the input, add it first
      if (emailValue && emailRegex.test(emailValue)) {
        await this.addEmail(event, id);

        // Wait a bit for the form to update
        await new Promise(resolve => setTimeout(resolve, 100));
      }

      // Check if there are members to invite AFTER adding the email
      if (this.members.length === 0) {
        alert('Please add at least one email address');
        return;
      }

      // Remove duplicates
      this.removeDuplicateData();

      // Move to next step
      this.currentStep = 2;
      this.detectChange.detectChanges();

    } catch (error) {

      alert('An error occurred. Please try again.');
    }
  }

  /**
   * De-duplicate the staged members (same email + role) and rebuild the
   * members FormArray from the unique set.
   */
  removeDuplicateData() {
    this.receiveMembersData = this.members.value

    this.receiveMembersData?.forEach((item:any) => {

      const isDuplicate = this.notDuplicateMemberData?.some( // search duplicate member data
        (memberItem) =>
          memberItem.memberEmail === item.memberEmail && memberItem.role === item.role
      );

      if (!isDuplicate) { // if not duplicate
        this.notDuplicateMemberData?.push(item);
      }
    })

    this.members.clear()

    // Push not duplicate member data into formArray members
    this.notDuplicateMemberData?.forEach((item:any) => {
      this.members.push(this.formBuilder.group({
        memberEmail: [item.memberEmail],
        role: [item.role]
      }))
    })
  }

  /** Go back to the email-entry step (step 1). */
  prevStep() {
    this.currentStep = 1;
    this.detectChange.detectChanges()
  }

  /**
   * Generate a random hex color for a member avatar.
   * @returns a color string such as '#A3F2C1'
   */
  getRandomColor(): string {
    const letters = '0123456789ABCDEF';
    this.color = '#';
    for (let i = 0; i < 6; i++) {
      this.color += letters[Math.floor(Math.random() * 16)];
    }
    return this.color;
  }

  /**
   * Whether a role radio button should be disabled (another one is already
   * selected).
   * @param type - the radio group type ('task' or 'global')
   * @returns true if the radio should be disabled
   */
  isRadioDisabled(type: 'task' | 'global'): boolean {
    // Disables all other radios except the selected one
    if (type === 'task') {
      return this.selectedIndex !== null ;
    }

    return this.selectedIndex !== null;
  }

  /**
   * Select a radio button by index.
   * @param index - the index of the selected radio
   */
  selectRadio(index: number): void {
    this.selectedIndex = index;
  }

  /**
   * Invite the staged members to the board: filters out members already on
   * the board, persists the new ones, then resets the form and reloads.
   * @param event - the form submit event
   * @param id - the board id
   */
  async handleInviteMember(event: any, id: string) {
    event.preventDefault();

    try {
      if (!this.members?.length) {
        alert('Please add at least one member to invite');
        return;
      }

      if (!id) {
        alert('Board ID is missing');
        return;
      }

      const choosedRole = this.chooseRole.value;
      this.members.controls.forEach((control) => {
        control.get('role')?.setValue(choosedRole);
      });

      // Receive existing members for this board to check if email already exist for this user on this board
      const existingMembers = await lastValueFrom(
        this.memberService.getMembersByBoard<Members>(
          'board/member',
          id
        )
      );

      const existingEmailsForUser = new Set(
        existingMembers
          .map(m => m.memberEmail?.toLowerCase())
          .filter((e): e is string => !!e)
      );

      const memberData: Members[] = this.members.value;
      this.newMembers = [];
      let addedCount = 0;

      // Add only new members that are not already part of the board for this user
      memberData.forEach((item: Members) => {
        const email = item.memberEmail?.toLowerCase();
        if (email && !existingEmailsForUser.has(email)) {
          this.newMembers.push({
            memberEmail: item.memberEmail,
            role: item.role || 'Standard',
            boardId: id
          });
          addedCount++;
        }
      });

      if (addedCount === 0 || this.newMembers.length === 0) {
        alert('All members are already part of this board for this user');
        return;
      }

      // Add new members to the Member table
      for (const member of this.newMembers) {
        await lastValueFrom(
          this.memberService.addMemberData('board/member', member)
        );
      }

      // Reset form
      setTimeout(() => {
        this.members.clear();
        this.notDuplicateMemberData = [];
        this.currentStep = 1;
        this.form.reset({ chooseRole: 'Standard' });
        this.closeInviteModal.emit();
        this.reloadPage.reloadPage();
      }, 0);

    } catch (error: any) {

      if (error?.status === 0) {
        alert('Cannot connect to the server. Please check if the Server is running.');
      } else {
        alert(`An error occurred while inviting members: ${error?.message || 'Unknown error'}. Please check the console for details.`);
      }
    }
  }

}
