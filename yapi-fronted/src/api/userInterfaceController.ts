// @ts-ignore
/* eslint-disable */
import request from '@/request.ts'

/** 此处后端没有提供注释 POST /delete */
export async function deleteUserInterface(
  body: API.DeleteRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/delete', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /list/page */
export async function listUserInterfaceByPage(
  body: API.UserInterfaceQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageUserInterface>('/userInterface/list/page', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /list/page/admin */
export async function listUserInterfaceByPageByAdmin(
  body: API.UserInterfaceQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageUserInterface>('/userInterface/list/page/admin', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
