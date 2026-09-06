// @ts-ignore
/* eslint-disable */
import request from '@/request.ts'

/** 此处后端没有提供注释 POST /add */
export async function addInterfaceInfo(
  body: API.InterfaceInfoAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/interfaceInfo/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /addUserInterface */
export async function addUserInterface(
  body: API.UserInterfaceAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/interfaceInfo/addUserInterface', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /apply */
export async function applyInterface(
  body: API.UserInterfaceApplyRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/interfaceInfo/apply', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /delete */
export async function deleteInterface(body: API.DeleteRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/interfaceInfo/delete', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /GetInterface/${param0} */
export async function getInterfaceById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getInterfaceByIdParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params
  return request<API.BaseResponseInterfaceInfoVO>(`/interfaceInfo/getInterface/${param0}`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /hadApply */
export async function listUserInterfaceApply(options?: { [key: string]: any }) {
  return request<API.BaseResponseListInterfaceInfoVO>('/interfaceInfo/hadApply', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /hadCreate */
export async function listInterfaceCreate(options?: { [key: string]: any }) {
  return request<API.BaseResponseListInterfaceInfoVO>('/interfaceInfo/hadCreate', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /invoke */
export async function invokeInterface(
  body: API.InterfaceInfoInvokeRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseString>('/interfaceInfo/invoke', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /list/page */
export async function listInterfaceByPage(
  body: API.InterfaceInfoQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageInterfaceInfo>('/interfaceInfo/list/page', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /list/page/admin */
export async function listInterfaceByPageByAdmin(
  body: API.InterfaceInfoQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageInterfaceInfo>('/interfaceInfo/list/page/admin', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /offline */
export async function offlineInterface(body: API.DeleteRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/interfaceInfo/offline', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /publish */
export async function publishInterface(body: API.IdRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean>('/interfaceInfo/publish', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /rank */
export async function listInterfaceRank(options?: { [key: string]: any }) {
  return request<API.BaseResponseListInterfaceRankVO>('/interfaceInfo/rank', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /update */
export async function updateInterfaceInfo(
  body: API.InterfaceInfoUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/interfaceInfo/update', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /updateUserInterface */
export async function updateUserInterface(
  body: API.UserInterfaceUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean>('/interfaceInfo/updateUserInterface', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
