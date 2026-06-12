export interface MediaModel {
  id: number;
  name: string;
  description: string;
  url: string;       // relative path — combine with serverBaseUrl
  type: string;
  range: number;
  share: boolean;
  size: number;
  timestmp: Date;
}
