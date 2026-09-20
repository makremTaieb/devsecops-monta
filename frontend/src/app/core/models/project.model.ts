export const VcsType = {
  GITHUB:    'GITHUB',
  GITLAB:    'GITLAB',
  BITBUCKET: 'BITBUCKET',
} as const;
export type VcsType = typeof VcsType[keyof typeof VcsType];

export interface Project {
  id:            number;
  name:          string;
  repositoryUrl: string;
  branch:        string;
  owner?:        string;
  createdBy:     string;
  vcsType:       VcsType;
  createdAt:     string;
  status?:       string;
  description?:  string;
}

export interface CreateProjectRequest {
  name:          string;
  repositoryUrl: string;
  branch:        string;
  owner?:        string;
  vcsType:       VcsType;
}
