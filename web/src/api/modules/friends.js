import { request } from '../httpClient'

export const listFriends = () => request('/friends')
export const searchFriendByPublicId = publicId => request('/friends/search', { params: { publicId } })
export const sendFriendRequest = publicId => request('/friends/requests', {
  method: 'POST',
  body: { publicId }
})

export const listReceivedFriendRequests = () => request('/friends/requests/received')
export const acceptFriendRequest = id => request(`/friends/requests/${id}/accept`, { method: 'POST' })
export const rejectFriendRequest = id => request(`/friends/requests/${id}/reject`, { method: 'POST' })
