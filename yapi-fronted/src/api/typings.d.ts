declare namespace API {
  // ==================== 通用基础类型 ====================
  // 注意：id / userId / interfaceId 等字段为雪花 ID（Long），已统一改为 string，
  // 避免超过 JS Number 安全整数范围（Number.MAX_SAFE_INTEGER）导致精度丢失。

  type BaseResponseBoolean = {
    code?: number
    data?: boolean
    message?: string
  }

  type BaseResponseLong = {
    code?: number
    data?: string
    message?: string
  }

  type BaseResponseString = {
    code?: number
    data?: string
    message?: string
  }

  type DeleteRequest = {
    id?: string
  }

  type IdRequest = {
    id?: string
  }

  type OrderItem = {
    column?: string
    asc?: boolean
  }

  // ==================== 接口（interfaceInfoController） ====================

  type BaseResponseInterfaceInfoVO = {
    code?: number
    data?: InterfaceInfoVO
    message?: string
  }

  type BaseResponseListInterfaceInfoVO = {
    code?: number
    data?: InterfaceInfoVO[]
    message?: string
  }

  type BaseResponseListInterfaceRankVO = {
    code?: number
    data?: InterfaceRankVO[]
    message?: string
  }

  type BaseResponsePageInterfaceInfo = {
    code?: number
    data?: PageInterfaceInfo
    message?: string
  }

  type getInterfaceByIdParams = {
    id: string
  }

  type InterfaceInfo = {
    id?: string
    interfaceName?: string
    description?: string
    path?: string
    url?: string
    requestHeader?: string
    requestParams?: string
    responseHeader?: string
    status?: number
    method?: string
    userId?: string
    createTime?: string
    updateTime?: string
    isDelete?: number
  }

  type InterfaceInfoAddRequest = {
    interfaceName?: string
    description?: string
    url?: string
    requestHeader?: string
    requestParams?: string
    responseHeader?: string
    method?: string
  }

  type InterfaceInfoInvokeRequest = {
    path?: string
    method?: string
    requestParams?: string
  }

  type InterfaceInfoQueryRequest = {
    current?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: string
    interfaceName?: string
    description?: string
    url?: string
    path?: string
    requestHeader?: string
    requestParams?: string
    responseHeader?: string
    method?: string
    status?: number
    userId?: string
  }

  type InterfaceInfoUpdateRequest = {
    id?: string
    interfaceName?: string
    description?: string
    url?: string
    path?: string
    requestHeader?: string
    requestParams?: string
    responseHeader?: string
    method?: string
  }

  type InterfaceInfoVO = {
    id?: string
    interfaceName?: string
    description?: string
    totalNum?: number
    leftNum?: number
    applicantCount?: string
    path?: string
    url?: string
    requestHeader?: string
    requestParams?: string
    responseHeader?: string
    status?: number
    method?: string
    userId?: string
    createTime?: string
    updateTime?: string
    user?: UserVO
  }

  type InterfaceRankVO = {
    id?: string
    interfaceName?: string
    description?: string
    invokeCount?: number
  }

  type PageInterfaceInfo = {
    records?: InterfaceInfo[]
    total?: number
    size?: number
    current?: number
    orders?: OrderItem[]
    optimizeCountSql?: boolean
    searchCount?: boolean
    optimizeJoinOfCountSql?: boolean
    countId?: string
    maxLimit?: number
    pages?: number
  }

  type UserInterfaceAddRequest = {
    userId?: string
    interfaceId?: string
    leftNum?: number
    status?: number
    createTime?: string
  }

  type UserInterfaceApplyRequest = {
    id?: string
    userId?: string
    interfaceId?: string
  }

  type UserInterfaceUpdateRequest = {
    id?: string
    userId?: string
    interfaceId?: string
    totalNum?: number
    leftNum?: number
    status?: number
  }

  // ==================== 用户（userController） ====================

  type BaseResponseLoginUserVO = {
    code?: number
    data?: LoginUserVO
    message?: string
  }

  type BaseResponsePageUser = {
    code?: number
    data?: PageUser
    message?: string
  }

  type BaseResponsePageUserVO = {
    code?: number
    data?: PageUserVO
    message?: string
  }

  type BaseResponseUser = {
    code?: number
    data?: User
    message?: string
  }

  type BaseResponseUserVO = {
    code?: number
    data?: UserVO
    message?: string
  }

  type getUserByIdParams = {
    id: string
  }

  type getUserVOByIdParams = {
    id: string
  }

  type LoginUserVO = {
    id?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    createTime?: string
    updateTime?: string
  }

  type PageUser = {
    records?: User[]
    total?: number
    size?: number
    current?: number
    orders?: OrderItem[]
    optimizeCountSql?: boolean
    searchCount?: boolean
    optimizeJoinOfCountSql?: boolean
    countId?: string
    maxLimit?: number
    pages?: number
  }

  type PageUserVO = {
    records?: UserVO[]
    total?: number
    size?: number
    current?: number
    orders?: OrderItem[]
    optimizeCountSql?: boolean
    searchCount?: boolean
    optimizeJoinOfCountSql?: boolean
    countId?: string
    maxLimit?: number
    pages?: number
  }

  type User = {
    id?: string
    userAccount?: string
    userPassword?: string
    accessKey?: string
    secretKey?: string
    userName?: string
    gender?: number
    userAvatar?: string
    userProfile?: string
    userRole?: string
    createTime?: string
    updateTime?: string
    isDelete?: number
  }

  type UserAddRequest = {
    userName?: string
    userAccount?: string
    userAvatar?: string
    userRole?: string
  }

  type UserLoginRequest = {
    userAccount?: string
    userPassword?: string
  }

  type UserQueryRequest = {
    current?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: string
    userName?: string
    userProfile?: string
    userRole?: string
  }

  type UserRegisterRequest = {
    userAccount?: string
    userPassword?: string
    checkPassword?: string
  }

  type UserUpdateMyRequest = {
    userName?: string
    userAvatar?: string
    userProfile?: string
  }

  type UserUpdateRequest = {
    id?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
  }

  type UserVO = {
    id?: string
    userName?: string
    userAvatar?: string
    userProfile?: string
    userRole?: string
    createTime?: string
  }

  // ==================== 用户-接口（userInterfaceController） ====================

  type BaseResponsePageUserInterface = {
    code?: number
    data?: PageUserInterface
    message?: string
  }

  type PageUserInterface = {
    records?: UserInterface[]
    total?: number
    size?: number
    current?: number
    orders?: OrderItem[]
    optimizeCountSql?: boolean
    searchCount?: boolean
    optimizeJoinOfCountSql?: boolean
    countId?: string
    maxLimit?: number
    pages?: number
  }

  type UserInterface = {
    id?: string
    userId?: string
    interfaceId?: string
    totalNum?: number
    leftNum?: number
    status?: number
    createTime?: string
    updateTime?: string
    isDelete?: number
  }

  type UserInterfaceQueryRequest = {
    current?: number
    pageSize?: number
    sortField?: string
    sortOrder?: string
    id?: string
    userId?: string
    interfaceId?: string
    totalNum?: number
    leftNum?: number
    status?: number
    createTime?: string
  }
}
