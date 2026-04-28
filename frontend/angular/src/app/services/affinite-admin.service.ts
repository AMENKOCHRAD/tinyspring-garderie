import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { UserCategorieScoreDto } from 'src/app/models/boutique/user-categorie-score.dto';

@Injectable({ providedIn: 'root' })
export class AffiniteAdminService {
  private readonly http = inject(HttpClient);
  private readonly adminUrl = 'http://localhost:8081/api/admin/boutique/affinites';

  getAllScores(): Observable<UserCategorieScoreDto[]> {
    return this.http.get<UserCategorieScoreDto[]>(this.adminUrl);
  }

  getScoresByUser(userId: number): Observable<UserCategorieScoreDto[]> {
    return this.http.get<UserCategorieScoreDto[]>(`${this.adminUrl}/user/${userId}`);
  }
}
