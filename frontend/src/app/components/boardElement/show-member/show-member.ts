import { ChangeDetectorRef, Component, ElementRef, EventEmitter, HostListener, Input, Output } from '@angular/core';
import { Members } from '../../../models/members.model';
import { EntityService } from '../../../services/entity-service';
import { Reload } from '../../../services/reload';
import { Message } from '../../../services/message';
import { lastValueFrom } from 'rxjs';
import { Board as BoardModel } from '../../../models/board.model';
import { CharacterPipe } from '../../../pipes/character-pipe';
import { ChangeRole } from "../change-role/change-role";
import { CommonModule } from '@angular/common';
import { MemberService } from '../../../services/member-service';
import { faPencil} from '@fortawesome/free-solid-svg-icons';
import {  } from '@fortawesome/free-regular-svg-icons';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';

@Component({
  selector: 'app-show-member',
  imports: [CommonModule, CharacterPipe, ChangeRole, FontAwesomeModule],
  templateUrl: './show-member.html',
  styleUrl: './show-member.css',
})
export class ShowMember {
  // Font Awesome icon
  readonly faPencil = faPencil;

  // 
  @Input() memberData!: Members[];
  @Input() getBoardId!: string;
  @Input() currentStep!: number;
  @Output() closeMemberModal = new EventEmitter<void>();
  @Output() sendChangeRoleToSearch = new EventEmitter<boolean>();
  @Output() clickOutside = new EventEmitter<boolean>();

  members?: Members[];
  selectedMember?: Members[];
  delete?: Members[];
  isChangeRole = false;
  dropdownValue = false;

  constructor(
    private memberService: MemberService,
    private reloadPage: Reload,
    private cdr: ChangeDetectorRef,
    private elementRef: ElementRef,
    private sendMemberData: Message
  ) {}

  /**
   * When a click occurs outside this component, toggle the dropdown state
   * and reset the change-role panel as needed.
   * @param event - the document click event
   */
  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.elementRef.nativeElement.contains(event.target)) {
      // Use setTimeout to avoid ExpressionChangedAfterItHasBeenCheckedError
      setTimeout(() => {
        if (this.dropdownValue === false) {
          this.clickOutside.emit(this.dropdownValue = true);

          if (this.isChangeRole === true && this.currentStep === 2) {
            this.isChangeRole = false;
          }
        } else {
          if (this.isChangeRole === false) {
            this.clickOutside.emit(this.dropdownValue = false);
          }

          if (this.isChangeRole === true && this.currentStep === undefined) {
            this.isChangeRole = false;
          }
        }
        this.cdr.detectChanges();
      }, 0);
    }

   
    
  }


  /**
   * Load the clicked member's full data and open the change-role panel.
   * @param event - the clicked member's email
   * @param id - unused, kept for template call-site compatibility
   */
  async handleMemberEdit(event: string | undefined, id: string | undefined): Promise<void> {
    if (!event || !id) return;
    
    try {
      const allMembers = await lastValueFrom(
        this.memberService.getMemberData<Members>('board/member')
      );

      if (!allMembers || !allMembers[0]) {
        console.error('Board not found');
        return;
      }

      this.selectedMember = allMembers?.filter(
        (item: Members) => item.memberEmail === event
      );
      
      // Show the change-role panel inside a setTimeout
      setTimeout(() => {
        this.isChangeRole = true;

        if (this.currentStep === 2) {
          this.sendChangeRoleToSearch.emit(true);
        }

        this.cdr.detectChanges();
      }, 0);

    } catch (error) {
      console.error('Error editing member:', error);
    }
  }

  /**
   * Remove a member from the board and reload the page.
   * @param event - object identifying the member to delete (must carry `id`)
   */
  async handlRemoveMember(event: any): Promise<void> {
    if (!event) return;
    try {

      // Delete member
      await lastValueFrom(
        this.memberService.delete('board/member', event)
      );
      
      this.reloadPage.reloadPage();
    
    } catch (error) {
      console.error('Error removing member:', error);
      alert('An error occurred while removing the member');
    }
  }

  /** Close the show-member modal. */
  handleCloseDropdown(): void {
    this.closeMemberModal.emit();
  }

  /**
   * Close the nested change-role panel, optionally applying the updated
   * member data.
   * @param event - the updated member if a role change was saved, or
   *                undefined if the panel was simply closed
   */
  handleCloseChangeRole(event: Members | undefined): void {
    // Use setTimeout to avoid ExpressionChangedAfterItHasBeenCheckedError
    setTimeout(() => {
      if (event === undefined) {
        // Change role hidden
        this.isChangeRole = false;
      } else {
        // Refresh the local memberData with the updated member
        this.memberData = [event];
        this.sendMemberData.messageRoleChange(event);

        // Change role hidden
        this.isChangeRole = false;
      }

      this.sendChangeRoleToSearch.emit(this.isChangeRole);
      this.cdr.detectChanges();
    }, 0);
  }

}
