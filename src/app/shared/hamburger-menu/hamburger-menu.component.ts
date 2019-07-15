import { LogoutService } from './../../core/services/logout.service';
import { Component, OnInit, Input, ViewEncapsulation} from '@angular/core';
import { Location } from '@angular/common';

@Component({
  selector: 'app-hamburger-menu',
  templateUrl: './hamburger-menu.component.html',
  styleUrls: ['./hamburger-menu.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class HamburgerComponent implements OnInit {

  @Input() data: any;

  dataList: any[];

  constructor(private location: Location, private logoutService: LogoutService) { }

  ngOnInit() {
    console.log('hfdasfdsf', this.data);
    if (this.data && this.data.menuList) {
      this.dataList = this.data.menuList;
      console.log('ButtonName', this.dataList);
    }

  }

  onItem() {
    this.logoutService.logout();
  }

}
